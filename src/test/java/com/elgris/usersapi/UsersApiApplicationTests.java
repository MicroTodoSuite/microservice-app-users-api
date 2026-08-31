package com.elgris.usersapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elgris.usersapi.configuration.OperationalProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "jwt.secret=unit-test-secret",
        "management.prometheus.metrics.export.enabled=true",
        "management.zipkin.tracing.export.enabled=false"
})
@AutoConfigureMockMvc
class UsersApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Autowired
    private OperationalProperties operationalProperties;

    @Test
    void contextLoads() {
    }

    @Test
    void prometheusEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("jvm_")));
    }

    @Test
    void actuatorStartupReadinessAndLivenessGroupsArePublic() throws Exception {
        for (String path : new String[] {"/health/startup", "/health/readiness", "/health/liveness"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    @Test
    void readinessCanFailWithoutFailingLiveness() throws Exception {
        try {
            AvailabilityChangeEvent.publish(applicationContext, ReadinessState.REFUSING_TRAFFIC);

            mockMvc.perform(get("/health/readiness"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
            mockMvc.perform(get("/health/liveness"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        } finally {
            AvailabilityChangeEvent.publish(applicationContext, ReadinessState.ACCEPTING_TRAFFIC);
        }
    }

    @Test
    void callerCorrelationIdIsEchoed() throws Exception {
        mockMvc.perform(get("/health/liveness")
                        .header("X-Request-Id", "caller-request-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "caller-request-123"));
    }

    @Test
    void correlationIdIsGeneratedWhenAbsent() throws Exception {
        mockMvc.perform(get("/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", Matchers.matchesPattern("[A-Za-z0-9._:-]+")));
    }

    @Test
    void openTelemetryBridgeIsAvailable() {
        org.junit.jupiter.api.Assertions.assertNotNull(openTelemetry);
    }

    @Test
    void operationalConfigurationDefaultsOffAndContainsNoJwtSecret() throws Exception {
        org.junit.jupiter.api.Assertions.assertFalse(
                operationalProperties.features().verboseSecurityErrors(),
                "security error detail must stay behind an explicit default-off toggle");
        String rendered = objectMapper.writeValueAsString(operationalProperties);
        org.junit.jupiter.api.Assertions.assertFalse(rendered.contains("unit-test-secret"));
        org.junit.jupiter.api.Assertions.assertFalse(rendered.contains("jwt"));
    }

    @Test
    void userEndpointRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/users/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void matchingJwtSubjectCanReadTheExistingH2User() throws Exception {
        mockMvc.perform(get("/users/admin")
                        .header("Authorization", "Bearer " + tokenFor("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.firstname").value("Foo"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void jwtSubjectCannotReadAnotherUser() throws Exception {
        mockMvc.perform(get("/users/janed")
                        .header("Authorization", "Bearer " + tokenFor("admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedSignedJwtIsRejectedInsteadOfEscapingAsServerError() throws Exception {
        mockMvc.perform(get("/users/admin")
                        .header("Authorization", "Bearer " + signedTokenWithPayload("not-json")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void serviceOwnedAndHttpMetricsAreExported() throws Exception {
        mockMvc.perform(get("/count")
                        .header("Authorization", "Bearer " + tokenFor("admin")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("count_requests_total")))
                .andExpect(content().string(Matchers.containsString("http_server_requests_seconds_count")));
    }

    private String tokenFor(String username) throws Exception {
        return signedTokenWithPayload(objectMapper.writeValueAsString(Map.of(
                "username", username,
                "scope", "read",
                "exp", Instant.now().plusSeconds(300).getEpochSecond())));
    }

    private String signedTokenWithPayload(String payloadJson) throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
        String payload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("unit-test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = encoder.encodeToString(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.US_ASCII)));
        return header + "." + payload + "." + signature;
    }
}
