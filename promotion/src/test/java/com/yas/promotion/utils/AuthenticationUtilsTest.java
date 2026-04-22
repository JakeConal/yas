package com.yas.promotion.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticationUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extractUserId_whenJwtAuthenticationToken_thenReturnsSubject() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "user-123")
            .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        assertEquals("user-123", AuthenticationUtils.extractUserId());
    }

    @Test
    void extractUserId_whenAnonymousAuthentication_thenThrowsAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        ));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, AuthenticationUtils::extractUserId);

        assertEquals(Constants.ErrorCode.ACCESS_DENIED, exception.getMessage());
    }

    @Test
    void extractJwt_whenJwtAuthenticationToken_thenReturnsTokenValue() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "user-456")
            .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        assertEquals("token", AuthenticationUtils.extractJwt());
    }
}