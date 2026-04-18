package com.yas.commonlibrary.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yas.commonlibrary.exception.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

class AuthenticationUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extractUserId_whenAnonymous_shouldThrowAccessDeniedException() {
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThrows(AccessDeniedException.class, AuthenticationUtils::extractUserId);
    }

    @Test
    void extractUserId_withJwtToken_shouldReturnSubject() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", "user-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(jwtToken);

        String userId = AuthenticationUtils.extractUserId();
        assertEquals("user-123", userId);
    }

    @Test
    void extractJwt_withJwtToken_shouldReturnTokenValue() {
        Jwt jwt = Jwt.withTokenValue("my-jwt-token-value")
            .header("alg", "RS256")
            .claim("sub", "user-456")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(jwtToken);

        String tokenValue = AuthenticationUtils.extractJwt();
        assertEquals("my-jwt-token-value", tokenValue);
    }

    @Test
    void getAuthentication_shouldReturnCurrentAuthentication() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", "user-789")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(jwtToken);

        assertNotNull(AuthenticationUtils.getAuthentication());
        assertEquals(jwtToken, AuthenticationUtils.getAuthentication());
    }
}
