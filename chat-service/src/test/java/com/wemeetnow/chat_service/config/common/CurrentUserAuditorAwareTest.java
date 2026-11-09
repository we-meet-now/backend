package com.wemeetnow.chat_service.config.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserAuditorAwareTest {

    private final CurrentUserAuditorAware auditorAware = new CurrentUserAuditorAware();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedJwtUserIdAsString() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        42L, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(auditorAware.getCurrentAuditor()).contains("42");
    }

    @Test
    void returnsEmptyWhenAuthenticationIsMissing() {
        SecurityContextHolder.clearContext();

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    void returnsEmptyForAnonymousAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }
}
