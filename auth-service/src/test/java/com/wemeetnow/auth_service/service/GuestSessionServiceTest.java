package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.GuestSession;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.ErrorCode;
import com.wemeetnow.auth_service.dto.GuestSessionResponseDto;
import com.wemeetnow.auth_service.exception.CustomException;
import com.wemeetnow.auth_service.repository.GuestSessionRepository;
import com.wemeetnow.auth_service.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuestSessionServiceTest {
    private static final String SECRET = "01234567890123456789012345678901";

    private UserRepository userRepository;
    private GuestSessionRepository guestSessionRepository;
    private JwtUtil jwtUtil;
    private UserService userService;
    private Object originalSecret;

    @BeforeEach
    void setUp() {
        originalSecret = ReflectionTestUtils.getField(JwtUtil.class, "SECRET_KEY");
        ReflectionTestUtils.setField(JwtUtil.class, "SECRET_KEY", SECRET);
        userRepository = mock(UserRepository.class);
        guestSessionRepository = mock(GuestSessionRepository.class);
        jwtUtil = mock(JwtUtil.class);
        userService = new UserService(
                mock(BCryptPasswordEncoder.class),
                userRepository,
                jwtUtil,
                guestSessionRepository
        );
    }

    @AfterEach
    void restoreSecret() {
        ReflectionTestUtils.setField(JwtUtil.class, "SECRET_KEY", originalSecret);
    }

    @Test
    void createsGuestSessionAndPersistsOnlyRefreshTokenHash() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(jwtUtil.generateGuestAccessToken(anyString())).thenReturn("short-lived-guest-token");
        when(guestSessionRepository.save(any(GuestSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GuestSessionResponseDto response = userService.createGuestSession();

        assertThat(response.getUuId()).isNotBlank();
        assertThat(response.getGuestToken()).isEqualTo("short-lived-guest-token");
        assertThat(response.getRefreshToken()).hasSize(43).isNotEqualTo(response.getGuestToken());
        assertThat(response.getGuestTokenExpiresInSeconds()).isEqualTo(1800);
        org.mockito.ArgumentCaptor<GuestSession> sessionCaptor =
                org.mockito.ArgumentCaptor.forClass(GuestSession.class);
        org.mockito.Mockito.verify(guestSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getRefreshTokenHash())
                .hasSize(64)
                .isNotEqualTo(response.getRefreshToken());
        assertThat(sessionCaptor.getValue().getUser().getUuid()).isEqualTo(response.getUuId());
    }

    @Test
    void rotatesRefreshTokenAndRejectsItsPreviousValue() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(jwtUtil.generateGuestAccessToken(anyString()))
                .thenReturn("first-access-token", "second-access-token");
        when(guestSessionRepository.save(any(GuestSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GuestSessionResponseDto created = userService.createGuestSession();
        org.mockito.ArgumentCaptor<GuestSession> sessionCaptor =
                org.mockito.ArgumentCaptor.forClass(GuestSession.class);
        org.mockito.Mockito.verify(guestSessionRepository).save(sessionCaptor.capture());
        GuestSession session = sessionCaptor.getValue();
        String oldHash = session.getRefreshTokenHash();

        when(guestSessionRepository.findActiveByRefreshTokenHashForUpdate(anyString()))
                .thenAnswer(invocation -> session.getRefreshTokenHash().equals(invocation.getArgument(0))
                        ? Optional.of(session)
                        : Optional.empty());

        GuestSessionResponseDto refreshed = userService.refreshGuestSession(created.getRefreshToken());

        assertThat(refreshed.getUuId()).isEqualTo(created.getUuId());
        assertThat(refreshed.getGuestToken()).isEqualTo("second-access-token");
        assertThat(refreshed.getRefreshToken()).isNotEqualTo(created.getRefreshToken());
        assertThat(session.getRefreshTokenHash()).hasSize(64).isNotEqualTo(oldHash);
        assertThatThrownBy(() -> userService.refreshGuestSession(created.getRefreshToken()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GUEST_SESSION_UNAUTHORIZED);
    }

    @Test
    void rejectsUnknownRefreshToken() {
        when(guestSessionRepository.findActiveByRefreshTokenHashForUpdate(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.refreshGuestSession("unknown-refresh-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GUEST_SESSION_UNAUTHORIZED);
    }

    @Test
    void upgradesGuestOnlyWhenUuidMatchesAnUnexpiredGuestToken() {
        String uuid = "existing-guest-uuid";
        String token = createGuestToken(uuid, Instant.now().plusSeconds(600));
        User guestUser = new User(uuid);
        guestUser.setId(42L);
        when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(guestUser));
        when(jwtUtil.generateGuestAccessToken(uuid)).thenReturn("new-access-token");
        when(guestSessionRepository.save(any(GuestSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GuestSessionResponseDto response = userService.upgradeGuestSession(uuid, token);

        assertThat(response.getUuId()).isEqualTo(uuid);
        assertThat(response.getGuestToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isNotBlank();
        org.mockito.ArgumentCaptor<GuestSession> sessionCaptor =
                org.mockito.ArgumentCaptor.forClass(GuestSession.class);
        verify(guestSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getUser()).isSameAs(guestUser);
    }

    @Test
    void rejectsUpgradeWhenUuidDoesNotMatchGuestToken() {
        String token = createGuestToken("signed-uuid", Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> userService.upgradeGuestSession("different-uuid", token))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GUEST_SESSION_UNAUTHORIZED);

        verify(userRepository, never()).findByUuid(anyString());
    }

    private String createGuestToken(String uuid, Instant expiration) {
        return Jwts.builder()
                .claim("uuId", uuid)
                .setExpiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
