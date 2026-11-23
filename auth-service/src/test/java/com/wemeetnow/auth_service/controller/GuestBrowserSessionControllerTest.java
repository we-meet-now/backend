package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.dto.GuestBrowserSessionResponseDto;
import com.wemeetnow.auth_service.dto.GuestBrowserSessionIdentityDto;
import com.wemeetnow.auth_service.exception.CustomException;
import com.wemeetnow.auth_service.service.GuestBrowserSessionService;
import com.wemeetnow.auth_service.service.GuestBrowserSessionService.IssuedSession;
import com.wemeetnow.auth_service.service.UserLogPersistenceService;
import com.wemeetnow.auth_service.service.UserLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuestBrowserSessionControllerTest {
    private static final String INTERNAL_SECRET = "01234567890123456789012345678901";
    private GuestBrowserSessionService sessionService;
    private GuestBrowserSessionController controller;

    @BeforeEach
    void setUp() {
        sessionService = mock(GuestBrowserSessionService.class);
        controller = new GuestBrowserSessionController(
                sessionService,
                new UserLogService(mock(UserLogPersistenceService.class))
        );
        ReflectionTestUtils.setField(controller, "cookieSecure", true);
        ReflectionTestUtils.setField(controller, "cookieSameSite", "Lax");
        ReflectionTestUtils.setField(controller, "internalSecret", INTERNAL_SECRET);
    }

    @Test
    void createsHttpOnlySecureSameSiteCookieWithoutReturningSessionId() {
        GuestBrowserSessionResponseDto body = GuestBrowserSessionResponseDto.builder()
                .expiresAt(Instant.now().plusSeconds(3600))
                .absoluteExpiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(sessionService.create()).thenReturn(IssuedSession.builder()
                .sessionId("opaque-session-value")
                .userId(42L)
                .uuid("guest-uuid")
                .response(body)
                .build());

        var response = controller.create(new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("guest_session=opaque-session-value")
                .contains("Path=/")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Domain=");
        assertThat(response.getBody().getData()).isSameAs(body);
    }

    @Test
    void revokeClearsCookieEvenWhenItIsAlreadyMissing() {
        var response = controller.revoke(null, new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("guest_session=")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
    }

    @Test
    void resolveRequiresTheInternalChatServiceKey() {
        assertThatThrownBy(() -> controller.resolve(
                "opaque-session-value",
                "incorrect-internal-key",
                new MockHttpServletRequest()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void resolveReturnsIdentityOnlyForAnInternalCaller() {
        GuestBrowserSessionIdentityDto identity = GuestBrowserSessionIdentityDto.builder()
                .userId(42L)
                .uuid("guest-uuid")
                .username("guest-name")
                .build();
        when(sessionService.resolveAndTouch("opaque-session-value")).thenReturn(identity);

        var response = controller.resolve(
                "opaque-session-value",
                INTERNAL_SECRET,
                new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isSameAs(identity);
    }
}
