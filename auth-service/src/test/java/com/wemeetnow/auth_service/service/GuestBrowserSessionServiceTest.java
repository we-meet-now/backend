package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.domain.GuestBrowserSession;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.ErrorCode;
import com.wemeetnow.auth_service.dto.GuestBrowserSessionIdentityDto;
import com.wemeetnow.auth_service.exception.CustomException;
import com.wemeetnow.auth_service.repository.GuestBrowserSessionRepository;
import com.wemeetnow.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuestBrowserSessionServiceTest {
    private UserRepository userRepository;
    private GuestBrowserSessionRepository sessionRepository;
    private GuestBrowserSessionService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        sessionRepository = mock(GuestBrowserSessionRepository.class);
        service = new GuestBrowserSessionService(userRepository, sessionRepository);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(sessionRepository.save(any(GuestBrowserSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsOpaqueSessionAndPersistsOnlyItsHash() {
        GuestBrowserSessionService.IssuedSession issued = service.create();

        assertThat(issued.getSessionId()).hasSize(43);
        assertThat(issued.getUuid()).isNotBlank();
        assertThat(issued.getUserId()).isEqualTo(42L);
        assertThat(issued.getResponse().getExpiresAt()).isBefore(issued.getResponse().getAbsoluteExpiresAt());
        assertThat(issued.getResponse().toString()).doesNotContain(issued.getSessionId(), issued.getUuid());

        org.mockito.ArgumentCaptor<GuestBrowserSession> sessionCaptor =
                org.mockito.ArgumentCaptor.forClass(GuestBrowserSession.class);
        org.mockito.Mockito.verify(sessionRepository).save(sessionCaptor.capture());
        GuestBrowserSession saved = sessionCaptor.getValue();
        assertThat(saved.getSessionIdHash()).hasSize(64).isNotEqualTo(issued.getSessionId());
        assertThat(saved.getUser().getUuid()).isEqualTo(issued.getUuid());
    }

    @Test
    void resolvesSessionToIdentityAndSlidesIdleExpiration() {
        GuestBrowserSessionService.IssuedSession issued = service.create();
        GuestBrowserSession session = captureSavedSession();
        Instant originalExpiresAt = session.getExpiresAt();
        stubActiveSession(session);

        GuestBrowserSessionIdentityDto identity = service.resolveAndTouch(issued.getSessionId());

        assertThat(identity.getUserId()).isEqualTo(42L);
        assertThat(identity.getUuid()).isEqualTo(issued.getUuid());
        assertThat(session.getLastUsedAt()).isNotNull();
        assertThat(session.getExpiresAt()).isAfter(originalExpiresAt);
    }

    @Test
    void refreshRotatesSessionIdAndRejectsPreviousId() {
        GuestBrowserSessionService.IssuedSession issued = service.create();
        GuestBrowserSession session = captureSavedSession();
        stubActiveSession(session);

        GuestBrowserSessionService.IssuedSession refreshed = service.refresh(issued.getSessionId());

        assertThat(refreshed.getSessionId()).isNotEqualTo(issued.getSessionId());
        assertThat(session.getSessionIdHash()).hasSize(64).isNotEqualTo(refreshed.getSessionId());
        assertThatThrownBy(() -> service.refresh(issued.getSessionId()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GUEST_SESSION_UNAUTHORIZED);
    }

    @Test
    void rejectsExpiredSession() {
        User user = new User("expired-uuid");
        user.setId(42L);
        Instant now = Instant.now();
        GuestBrowserSession expired = new GuestBrowserSession(
                user,
                "expired-hash",
                now.minusSeconds(7200),
                now.minusSeconds(3600),
                now.plusSeconds(3600)
        );
        when(sessionRepository.findActiveBySessionIdHashForUpdate(anyString()))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resolveAndTouch("opaque-session-id"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GUEST_SESSION_UNAUTHORIZED);
    }

    @Test
    void revokeIsIdempotentWhenCookieIsMissing() {
        assertThat(service.revoke(null)).isEmpty();
    }

    private GuestBrowserSession captureSavedSession() {
        org.mockito.ArgumentCaptor<GuestBrowserSession> sessionCaptor =
                org.mockito.ArgumentCaptor.forClass(GuestBrowserSession.class);
        org.mockito.Mockito.verify(sessionRepository).save(sessionCaptor.capture());
        return sessionCaptor.getValue();
    }

    private void stubActiveSession(GuestBrowserSession session) {
        when(sessionRepository.findActiveBySessionIdHashForUpdate(anyString()))
                .thenAnswer(invocation -> session.getSessionIdHash().equals(invocation.getArgument(0))
                        ? Optional.of(session)
                        : Optional.empty());
    }
}
