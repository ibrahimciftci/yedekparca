package com.parca.yedekparca;

import com.parca.yedekparca.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = Base64.getEncoder().encodeToString(
            "ParcaHan-test-secret-key-at-least-thirty-two-bytes".getBytes());

    @Test
    void generatedTokenIsValidForMatchingUser() {
        JwtService service = service(60_000);
        String token = service.generateToken("test@example.com");
        assertEquals("test@example.com", service.extractEmail(token));
        assertTrue(service.isTokenValid(token, "test@example.com"));
        assertFalse(service.isTokenValid(token, "other@example.com"));
    }

    @Test
    void expiredAndMalformedTokensAreRejected() throws Exception {
        JwtService service = service(-1);
        String expired = service.generateToken("test@example.com");
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> service.extractEmail(expired));
        assertFalse(service.isTokenValid(expired, "test@example.com"));
        assertFalse(service.isTokenValid("not.a.jwt", "test@example.com"));
    }

    private JwtService service(long expiration) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", SECRET);
        ReflectionTestUtils.setField(service, "expiration", expiration);
        return service;
    }
}
