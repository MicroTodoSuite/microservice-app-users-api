package com.elgris.usersapi.api;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CounterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CounterController.class);
    private final Counter requests;
    private final AtomicInteger count = new AtomicInteger();

    public CounterController(MeterRegistry meterRegistry) {
        this.requests = Counter.builder("count_requests_total")
                .description("Total count of requests")
                .register(meterRegistry);
    }

    @GetMapping("/count")
    public int count() {
        requests.increment();
        int current = count.incrementAndGet();
        LOGGER.info("Request counter incremented to {}", current);
        return current;
    }
}
