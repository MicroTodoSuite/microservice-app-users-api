package com.elgris.usersapi;

import com.elgris.usersapi.configuration.OperationalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

/**
 * The entry point for the Users API application.
 * <p>
 * This class is responsible for initializing the Spring Boot application and configuring Prometheus metrics collection.
 * It uses annotations to enable Prometheus endpoints, metrics collection, and timing for web requests.
 * </p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class UsersApiApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersApiApplication.class);

    /**
     * The main method that starts the Spring Boot application.
     * <p>
     * This method initializes Prometheus default exports and then runs the Spring Boot application.
     * </p>
     * 
     * @param args Command-line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(UsersApiApplication.class, args);
    }

    @Bean
    ApplicationRunner logOperationalConfiguration(OperationalProperties properties) {
        return arguments -> LOGGER.info("Operational configuration loaded: {}", properties);
    }
}
