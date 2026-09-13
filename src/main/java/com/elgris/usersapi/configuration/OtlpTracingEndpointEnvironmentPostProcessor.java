package com.elgris.usersapi.configuration;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Points OTLP trace export at {@code OTEL_EXPORTER_OTLP_ENDPOINT} only when that variable
 * has a value.
 *
 * <p>A property placeholder cannot do this: {@code ${OTEL_EXPORTER_OTLP_ENDPOINT:}}
 * resolves to an empty string, and Spring Boot creates the OTLP exporter whenever
 * {@code management.otlp.tracing.endpoint} is present, empty or not (spec 010 research R6).
 * Without the variable, users-api runs with export disabled.
 */
public class OtlpTracingEndpointEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String ENDPOINT_VARIABLE = "OTEL_EXPORTER_OTLP_ENDPOINT";

    static final String ENDPOINT_PROPERTY = "management.otlp.tracing.endpoint";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String endpoint = environment.getProperty(ENDPOINT_VARIABLE);
        if (StringUtils.hasText(endpoint) && !environment.containsProperty(ENDPOINT_PROPERTY)) {
            environment.getPropertySources()
                    .addLast(new MapPropertySource("otlpTracingEndpoint", Map.of(ENDPOINT_PROPERTY, endpoint)));
        }
    }
}
