package com.financialadvisor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Extracted from SecurityConfig to break circular dependency:
 * SecurityConfig → JwtAuthFilter → AppUserDetailsService → PasswordEncoder.
 */
@Configuration
public class PasswordConfig {

    private final AppProperties appProperties;

    public PasswordConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        int cost = appProperties.security().password().bcryptCost();
        return new BCryptPasswordEncoder(cost);
    }
}
