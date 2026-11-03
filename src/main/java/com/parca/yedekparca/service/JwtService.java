
package com.parca.yedekparca.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // JWT token oluşturur.
    public String generateToken(String email) {

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    // Token içerisinden kullanıcı e-postasını alır.
    public String extractEmail(String token) {

        return extractAllClaims(token).getSubject();
    }

    // Token'ın geçerli olup olmadığını kontrol eder.
    public boolean isTokenValid(String token, String email) {

        try {
            Claims claims = extractAllClaims(token);

            return claims.getSubject().equals(email)
                    && claims.getExpiration().after(new Date());

        } catch (Exception ex) {
            return false;
        }
    }

    // Token içerisindeki bilgileri çözer.
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Token geçerlilik süresini milisaniye olarak döndürür.
    public long getExpiration() {
        return expiration;
    }
}
