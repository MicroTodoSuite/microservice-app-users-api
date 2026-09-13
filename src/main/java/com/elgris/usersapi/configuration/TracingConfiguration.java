package com.elgris.usersapi.configuration;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationView;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Keeps kubelet probes and Prometheus scrapes out of traces (spec 010 FR-011) without
 * dropping their request metrics (FR-013).
 *
 * <p>An {@code ObservationPredicate} would silence the meter handler too, removing the
 * probes' {@code http_server_requests} series, and filtering finished spans would leave the
 * Spring Security observations nested in those requests as spans of their own. Instead,
 * this replaces Spring Boot's three tracing observation handlers, which it only creates
 * when missing, with ones that do not support a probe or scrape request or anything
 * observed inside one. Metrics handlers are untouched.
 */
@AutoConfiguration(after = OpenTelemetryTracingAutoConfiguration.class,
        before = MicrometerTracingAutoConfiguration.class)
@ConditionalOnBean(Tracer.class)
public class TracingConfiguration {

    @Bean
    @Order(MicrometerTracingAutoConfiguration.DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER)
    DefaultTracingObservationHandler probeSkippingDefaultTracingObservationHandler(Tracer tracer) {
        return new DefaultTracingObservationHandler(tracer) {
            @Override
            public boolean supportsContext(Observation.Context context) {
                return super.supportsContext(context) && !skipsTracing(context);
            }
        };
    }

    @Bean
    @ConditionalOnBean(Propagator.class)
    @Order(MicrometerTracingAutoConfiguration.RECEIVER_TRACING_OBSERVATION_HANDLER_ORDER)
    PropagatingReceiverTracingObservationHandler<?> probeSkippingReceiverTracingObservationHandler(Tracer tracer,
            Propagator propagator) {
        return new PropagatingReceiverTracingObservationHandler<>(tracer, propagator) {
            @Override
            public boolean supportsContext(Observation.Context context) {
                return super.supportsContext(context) && !skipsTracing(context);
            }
        };
    }

    @Bean
    @ConditionalOnBean(Propagator.class)
    @Order(MicrometerTracingAutoConfiguration.SENDER_TRACING_OBSERVATION_HANDLER_ORDER)
    PropagatingSenderTracingObservationHandler<?> probeSkippingSenderTracingObservationHandler(Tracer tracer,
            Propagator propagator) {
        return new PropagatingSenderTracingObservationHandler<>(tracer, propagator) {
            @Override
            public boolean supportsContext(Observation.Context context) {
                return super.supportsContext(context) && !skipsTracing(context);
            }
        };
    }

    /**
     * Micrometer's {@code TracingAwareMeterObservationHandler}, which records the request
     * metrics, requires a tracing context when an observation stops. A skipped observation
     * therefore still gets an empty one, so its metrics are recorded without a span.
     */
    static boolean skipsTracing(Observation.Context context) {
        if (!withinProbeOrScrape(context)) {
            return false;
        }
        context.computeIfAbsent(TracingObservationHandler.TracingContext.class,
                key -> new TracingObservationHandler.TracingContext());
        return true;
    }

    static boolean withinProbeOrScrape(Observation.ContextView context) {
        for (Observation.ContextView current = context; current != null; current = parentOf(current)) {
            if (current instanceof ServerRequestObservationContext request
                    && isProbeOrScrape(request.getCarrier().getRequestURI())) {
                return true;
            }
        }
        return false;
    }

    static boolean isProbeOrScrape(String path) {
        return path.equals("/prometheus") || path.equals("/health") || path.startsWith("/health/");
    }

    private static Observation.ContextView parentOf(Observation.ContextView context) {
        ObservationView parent = context.getParentObservation();
        return parent == null ? null : parent.getContextView();
    }
}
