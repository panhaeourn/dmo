package com.example.demo.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret-base64}")
    private String secretBase64;

    @Value("${app.jwt.expiry-ms:86400000}")
    private long expiryMs;

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Generate JWT with email as subject
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSignKey()) // HS256 inferred from key
                .compact();
    }

    /**
     * Simple validation (signature + expiry)
     */
    public boolean isValid(String token) {
        try {
            // parsing verifies signature automatically (because verifyWith)
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate token matches the given user
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public String getEmailFromToken(String token) {
        return extractEmail(token);
    }
}
