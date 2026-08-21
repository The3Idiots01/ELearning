package com.example.ELearningBE.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationMs", 900000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationMs", 604800000L);
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        Long userId = 101L;
        String role = "ADMIN";

        String token = jwtTokenProvider.generateAccessToken(userId, role);
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals(role, jwtTokenProvider.getRoleFromToken(token));
        assertNull(claims.get("email")); // verify email is not included in access token
    }

    @Test
    void testGenerateAndValidateRefreshToken() {
        Long userId = 202L;

        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(refreshToken));
    }
}
