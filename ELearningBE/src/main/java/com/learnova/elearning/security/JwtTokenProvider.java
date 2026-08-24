package com.learnova.elearning.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Getter
    @Value("${jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Getter
    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Getter
    @Value("${jwt.confirmation-token-expiration-ms:900000}")
    private long confirmationTokenExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Sinh Access Token chỉ chứa `id` và `role`
     */
    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("id", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Sinh Refresh Token
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("id", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Sinh Token xác thực tài khoản qua email (15 phút)
     */
    public String generateAccountConfirmationToken(String email, String tokenId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + confirmationTokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("type", "ACCOUNT_CONFIRMATION")
                .claim("email", email)
                .claim("tokenId", tokenId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Trích xuất email từ Confirmation Token
     */
    public String getEmailFromConfirmationToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            email = claims.getSubject();
        }
        return email;
    }

    /**
     * Trích xuất tokenId (UUID) từ Confirmation Token
     */
    public String getTokenIdFromConfirmationToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("tokenId", String.class);
    }

    /**
     * Kiểm tra tính hợp lệ của Confirmation Token
     */
    public boolean validateConfirmationToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        try {
            Claims claims = getClaimsFromToken(token);
            String type = claims.get("type", String.class);
            return "ACCOUNT_CONFIRMATION".equals(type);
        } catch (Exception e) {
            log.error("Failed to validate confirmation token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Trích xuất Claims từ JWT
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Lấy userId từ JWT
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object idClaim = claims.get("id");
        if (idClaim instanceof Number) {
            return ((Number) idClaim).longValue();
        }
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Lấy Role từ JWT
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * Kiểm tra tính hợp lệ của Token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature or format: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
