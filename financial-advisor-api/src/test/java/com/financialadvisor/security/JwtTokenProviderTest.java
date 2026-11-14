package com.financialadvisor.security;

import com.financialadvisor.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-20: JWT token generation and validation.
 * Valid credentials produce valid token; invalid credentials are rejected.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        var jwt = new AppProperties.Security.Jwt(
                "test-secret-must-be-at-least-32-bytes-long-for-hmac-sha256", 60
        );
        var cors = new AppProperties.Security.Cors("http://localhost:5173");
        var password = new AppProperties.Security.Password("bcrypt", 4);
        var encryption = new AppProperties.Security.Encryption("test-key-32-bytes-for-aes256!!");
        var security = new AppProperties.Security(jwt, cors, password, encryption);
        var financial = new AppProperties.Financial(RoundingMode.HALF_UP, 2, 7, 6);
        var props = new AppProperties(security, financial);

        jwtTokenProvider = new JwtTokenProvider(props);
    }

    @Test
    @DisplayName("generateToken produces a non-null, non-empty token")
    void generateToken_returnsValidToken() {
        String token = jwtTokenProvider.generateToken("testuser");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("validateToken returns true for a freshly generated token")
    void validateToken_validToken_returnsTrue() {
        String token = jwtTokenProvider.generateToken("testuser");
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("validateToken returns false for a tampered token")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtTokenProvider.generateToken("testuser");
        String tampered = token + "tampered";
        assertFalse(jwtTokenProvider.validateToken(tampered));
    }

    @Test
    @DisplayName("validateToken returns false for garbage input")
    void validateToken_garbageToken_returnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("not.a.jwt"));
    }

    @Test
    @DisplayName("getUsernameFromToken extracts the correct username")
    void getUsernameFromToken_returnsCorrectUsername() {
        String token = jwtTokenProvider.generateToken("admin");
        assertEquals("admin", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("generateRefreshToken produces a valid refresh token")
    void generateRefreshToken_isRecognizedAsRefresh() {
        String refresh = jwtTokenProvider.generateRefreshToken("testuser");
        assertTrue(jwtTokenProvider.validateToken(refresh));
        assertTrue(jwtTokenProvider.isRefreshToken(refresh));
    }

    @Test
    @DisplayName("Standard access token is not a refresh token")
    void generateToken_isNotRefreshToken() {
        String access = jwtTokenProvider.generateToken("testuser");
        assertFalse(jwtTokenProvider.isRefreshToken(access));
    }

    @Test
    @DisplayName("Constructor rejects secret shorter than 256 bits")
    void constructor_shortSecret_throwsException() {
        var jwt = new AppProperties.Security.Jwt("short", 60);
        var cors = new AppProperties.Security.Cors("http://localhost:5173");
        var password = new AppProperties.Security.Password("bcrypt", 4);
        var encryption = new AppProperties.Security.Encryption("test");
        var security = new AppProperties.Security(jwt, cors, password, encryption);
        var financial = new AppProperties.Financial(RoundingMode.HALF_UP, 2, 7, 6);
        var props = new AppProperties(security, financial);

        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(props));
    }
}
