package com.taiyuan.food.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationMs;

    public record JwtUser(String username, String role) {
    }

    public JwtUtil(@Value("${app.jwt.secret:change-me-to-a-long-random-string}") String secret,
                   @Value("${app.jwt.expiration-hours:24}") long hours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = hours * 3600_000L;
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(key)
            .compact();
    }

    public JwtUser parseToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload();
        return new JwtUser(claims.getSubject(), claims.get("role", String.class));
    }

    public boolean validate(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
