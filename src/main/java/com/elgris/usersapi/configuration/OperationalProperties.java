package com.elgris.usersapi.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Non-secret runtime configuration controlled independently from the image.
 * JWT material deliberately does not belong to this loggable object.
 */
@ConfigurationProperties(prefix = "users.operational")
public record OperationalProperties(Features features) {

    public OperationalProperties {
        features = features == null ? new Features(false) : features;
    }

    public record Features(boolean verboseSecurityErrors) {
    }
}
