package com.elgris.usersapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tracing contract for users-api (spec 010, T014).
 *
 * Tracing is off in Spring Boot tests unless {@link AutoConfigureObservability} turns it
 * on. Spans are read from an in-memory exporter the auto-configured batch processor
 * feeds, so these tests observe the production observation wiring.
 */
@SpringBootTest(properties = "jwt.secret=unit-test-secret")
@AutoConfigureMockMvc
@AutoConfigureObservability
class TracingConfigurationTests {

    private static final String INCOMING_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String INCOMING_SPAN_ID = "00f067aa0ba902b7";
    private static final String INCOMING_TRACEPARENT = "00-" + INCOMING_TRACE_ID + "-" + INCOMING_SPAN_ID + "-01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SdkTracerProvider tracerProvider;

    @Autowired
    private RecordingSpanExporter exporter;

    @BeforeEach
    void clearRecordedSpans() {
        flush();
        exporter.clear();
    }

    @Test
    void noZipkinExporterIsOnTheClasspath() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("io.opentelemetry.exporter.zipkin.ZipkinSpanExporter"));
    }

    @Test
    void anOtlpGrpcExporterIsConfiguredWhenTheEndpointIsSet() {
        try (ConfigurableApplicationContext context = start("OTEL_EXPORTER_OTLP_ENDPOINT=http://collector.test:4317")) {
            List<String> exporters = otlpExporters(context);
            assertEquals(1, exporters.size(), "want exactly one OTLP exporter, got " + exporters);
            assertTrue(exporters.get(0).startsWith("OtlpGrpcSpanExporter")
                    && exporters.get(0).contains("http://collector.test:4317"),
                    "want an OTLP/gRPC exporter to the configured endpoint, got " + exporters.get(0));
        }
    }

    @Test
    void noOtlpExporterIsConfiguredWhenTheEndpointIsEmpty() {
        try (ConfigurableApplicationContext context = start("OTEL_EXPORTER_OTLP_ENDPOINT=")) {
            assertEquals(List.of(), otlpExporters(context));
        }
    }

    @Test
    void probesAndScrapesProduceNoSpansButKeepTheirMetrics() throws Exception {
        for (String path : new String[] {"/health/startup", "/health/readiness", "/health/liveness", "/prometheus"}) {
            mockMvc.perform(get(path).header("traceparent", INCOMING_TRACEPARENT))
                    .andExpect(status().isOk());
        }

        assertEquals(List.of(), names(flush()), "probes and scrapes must not be traced");

        // Spec 010 FR-013: skipping their spans must not drop their request metrics.
        mockMvc.perform(get("/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.matchesPattern(
                        "(?s).*http_server_requests_seconds_count\\{[^}]*uri=\"/health/[^}]*\\}.*")))
                .andExpect(content().string(Matchers.matchesPattern(
                        "(?s).*http_server_requests_seconds_count\\{[^}]*uri=\"/prometheus\"[^}]*\\}.*")));
    }

    @Test
    void aUserLookupContinuesTheIncomingTraceWithoutCarryingTheToken() throws Exception {
        String token = tokenFor("admin");
        mockMvc.perform(get("/users/admin")
                        .header("Authorization", "Bearer " + token)
                        .header("traceparent", INCOMING_TRACEPARENT))
                .andExpect(status().isOk());

        List<SpanData> spans = flush();
        SpanData server = spans.stream().filter(span -> span.getKind() == SpanKind.SERVER).findFirst().orElse(null);
        assertNotNull(server, "want a SERVER span, got " + names(spans));
        assertEquals(INCOMING_TRACE_ID, server.getTraceId(), "the SERVER span must continue the incoming trace");
        assertEquals(INCOMING_SPAN_ID, server.getParentSpanId(), "the SERVER span must be a child of the caller's span");

        for (SpanData span : spans) {
            span.getAttributes().forEach((key, value) -> {
                String text = String.valueOf(value);
                assertFalse(text.contains(token) || text.toLowerCase(Locale.ROOT).contains("bearer"),
                        "span " + span.getName() + " carries a credential in attribute " + key.getKey());
            });
        }
    }

    private List<SpanData> flush() {
        tracerProvider.forceFlush().join(5, TimeUnit.SECONDS);
        return exporter.spans();
    }

    private static List<String> names(List<SpanData> spans) {
        return spans.stream().map(SpanData::getName).toList();
    }

    private static ConfigurableApplicationContext start(String endpointProperty) {
        return new SpringApplicationBuilder(UsersApiApplication.class)
                .properties("server.port=0", "jwt.secret=unit-test-secret", "spring.main.banner-mode=off",
                        endpointProperty)
                .run();
    }

    private static List<String> otlpExporters(ConfigurableApplicationContext context) {
        return context.getBeansOfType(SpanExporter.class).values().stream()
                .map(Object::toString)
                .filter(description -> description.startsWith("Otlp"))
                .toList();
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

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingExporterConfiguration {

        @Bean
        RecordingSpanExporter recordingSpanExporter() {
            return new RecordingSpanExporter();
        }
    }

    static final class RecordingSpanExporter implements SpanExporter {

        private final List<SpanData> spans = new CopyOnWriteArrayList<>();

        @Override
        public CompletableResultCode export(Collection<SpanData> batch) {
            spans.addAll(batch);
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }

        List<SpanData> spans() {
            return List.copyOf(spans);
        }

        void clear() {
            spans.clear();
        }
    }
}
