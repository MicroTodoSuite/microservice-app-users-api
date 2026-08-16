package com.elgris.usersapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The entry point for the Users API application.
 * <p>
 * This class is responsible for initializing the Spring Boot application and configuring Prometheus metrics collection.
 * It uses annotations to enable Prometheus endpoints, metrics collection, and timing for web requests.
 * </p>
 */
@SpringBootApplication
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
        // Run the Spring Boot application
        SpringApplication.run(UsersApiApplication.class, args);
    }    
}
