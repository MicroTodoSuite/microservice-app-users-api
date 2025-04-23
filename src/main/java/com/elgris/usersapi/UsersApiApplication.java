package com.elgris.usersapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import io.prometheus.client.spring.web.EnablePrometheusTiming;

/**
 * The entry point for the Users API application.
 * <p>
 * This class is responsible for initializing the Spring Boot application and configuring Prometheus metrics collection.
 * It uses annotations to enable Prometheus endpoints, metrics collection, and timing for web requests.
 * </p>
 */
@SpringBootApplication
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector
@EnablePrometheusTiming
public class UsersApiApplication {

    /**
     * The main method that starts the Spring Boot application.
     * <p>
     * This method initializes Prometheus default exports and then runs the Spring Boot application.
     * </p>
     * 
     * @param args Command-line arguments passed to the application.
     */
    public static void main(String[] args) {
        // Initialize Prometheus default exports for JVM metrics
        DefaultExports.initialize();
        
        // Run the Spring Boot application
        SpringApplication.run(UsersApiApplication.class, args);
    }    
}
