package com.agrogestao.security;

import com.agrogestao.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-that-is-at-least-32-bytes");
        properties.setExpirationMs(3_600_000L);
        jwtService = new JwtService(properties);
    }

    @Test
    void generateAndParseRoundTrip() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String email = "produtor@example.com";

        String token = jwtService.generateToken(userId, email);
        Claims claims = jwtService.parse(token);

        assertNotNull(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals(email, claims.get("email", String.class));
        assertEquals(userId, jwtService.extractUserId(token));
    }

    @Test
    void hmacRoundTripAcceptsMatchingSignature() {
        byte[] payload = "oauth-request".getBytes();
        String signature = jwtService.hmac(payload);

        assertTrue(jwtService.hmacMatches(payload, signature));
        assertFalse(jwtService.hmacMatches(payload, "tampered"));
        assertFalse(jwtService.hmacMatches("other".getBytes(), signature));
    }
}
