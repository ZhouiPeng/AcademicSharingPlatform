package com.academic.user.common;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil {
    private static final int expirationTime = 4 * 3600 * 1000;
    @Value("${JWT_SECRET}")
    private String jwtSecret;
    private SecretKey key;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not configured");
        }
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userId, String role) {
        Map<String, Object> inputClaims = new HashMap<>();
        inputClaims.put("userId", userId);
        inputClaims.put("role", role);
        String token = Jwts.builder()
                .claims(inputClaims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .id(UUID.randomUUID().toString())
                .signWith(key)
                .compact();
        return token;
    }

    public static String getExpirationTime() {
        return String.valueOf(expirationTime);
    }
}
