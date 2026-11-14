package com.financialadvisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.RoundingMode;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security, Financial financial) {

    public record Security(Jwt jwt, Cors cors, Password password, Encryption encryption) {

        public record Jwt(String secret, int expiryMinutes) {}

        public record Cors(String allowedOrigins) {}

        public record Password(String algorithm, int bcryptCost) {}

        public record Encryption(String key) {}
    }

    public record Financial(
            RoundingMode roundingMode,
            int scale,
            int stalenessThresholdDays,
            int emergencyMonthsThreshold
    ) {}
}
