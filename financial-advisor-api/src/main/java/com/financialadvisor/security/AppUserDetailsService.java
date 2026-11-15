package com.financialadvisor.security;

import com.financialadvisor.config.AppProperties;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Single-user UserDetailsService for V1 (per FR-1.4).
 * Credentials come from application config; future multi-user
 * phase will replace this with a DynamoDB-backed store.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final String configuredUsername;

    private final String encodedPassword;

    public AppUserDetailsService(AppProperties appProperties, PasswordEncoder passwordEncoder) {
        String rawPassword = System.getenv().getOrDefault("JWT_DEFAULT_PASSWORD", "changeme");
        this.configuredUsername = System.getenv().getOrDefault("JWT_DEFAULT_USER", "admin");
        this.encodedPassword = passwordEncoder.encode(rawPassword);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (configuredUsername.equals(username)) {
            return User.builder()
                    .username(configuredUsername)
                    .password(encodedPassword)
                    .roles("USER")
                    .build();
        }
        throw new UsernameNotFoundException("User not found: " + username);
    }
}
