package com.elgris.usersapi.api;

import io.prometheus.client.Counter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling requests related to counting.
 * <p>
 * This controller provides an endpoint to track and return the number of requests
 * received by the service. It also uses Prometheus to record the total number of requests.
 * </p>
 */
@RestController
public class CounterController {

    /** Counter to track the total number of requests, exposed to Prometheus. */
    private static final Counter requests = Counter.build()
            .name("count_requests_total")
            .help("Total count of requests.")
            .register();

    /** Counter to keep track of the number of requests in-memory. */
    private static int num = 0;

    /**
     * Handles GET requests to the "/count" endpoint.
     * <p>
     * This method increments the Prometheus counter for total requests and
     * returns the current count of requests received by the service.
     * </p>
     * 
     * @return The current count of requests, incremented by one.
     */
    @GetMapping("count")
    public int count() {
        // Increment the Prometheus counter for total requests
        requests.inc();
        
        // Increment and return the current request count
        return ++num;
    }
}
