package com.elgris.usersapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "jwt.secret=unit-test-secret",
        "management.prometheus.metrics.export.enabled=true"
})
@AutoConfigureMockMvc
class UsersApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void prometheusEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_")));
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

    private String tokenFor(String username) throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
        String payload = encoder.encodeToString(objectMapper.writeValueAsBytes(Map.of(
                "username", username,
                "scope", "read",
                "exp", Instant.now().plusSeconds(300).getEpochSecond())));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("unit-test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = encoder.encodeToString(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.US_ASCII)));
        return header + "." + payload + "." + signature;
    }
}
