package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.domain.GuestBrowserSession;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.ErrorCode;
import com.wemeetnow.auth_service.dto.GuestBrowserSessionIdentityDto;
import com.wemeetnow.auth_service.dto.GuestBrowserSessionResponseDto;
import com.wemeetnow.auth_service.exception.CustomException;
import com.wemeetnow.auth_service.repository.GuestBrowserSessionRepository;
import com.wemeetnow.auth_service.repository.UserRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static com.wemeetnow.auth_service.config.jwt.JwtExpirationEnums.GUEST_REFRESH_TOKEN_ABSOLUTE_EXPIRATION_TIME;
import static com.wemeetnow.auth_service.config.jwt.JwtExpirationEnums.GUEST_REFRESH_TOKEN_IDLE_EXPIRATION_TIME;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestBrowserSessionService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final GuestBrowserSessionRepository guestBrowserSessionRepository;

    @Transactional
    public IssuedSession create() {
        String uuid = UUID.randomUUID().toString();
        User user = userRepository.save(new User(uuid));
        if (user.getId() == null) {
            throw new CustomException(ErrorCode.GUEST_USER_CREATE_FAILED);
        }

        Instant now = Instant.now();
        Instant idleExpiresAt = now.plus(
                GUEST_REFRESH_TOKEN_IDLE_EXPIRATION_TIME.getValue(), ChronoUnit.MILLIS);
        Instant absoluteExpiresAt = now.plus(
                GUEST_REFRESH_TOKEN_ABSOLUTE_EXPIRATION_TIME.getValue(), ChronoUnit.MILLIS);
        String sessionId = generateSessionId();
        GuestBrowserSession session = new GuestBrowserSession(
                user,
                hashSessionId(sessionId),
                now,
                idleExpiresAt,
                absoluteExpiresAt
        );
        guestBrowserSessionRepository.save(session);
        return toIssuedSession(session, sessionId);
    }

    @Transactional
    public IssuedSession refresh(String sessionId) {
        Instant now = Instant.now();
        GuestBrowserSession session = findUsableSession(sessionId);
        String rotatedSessionId = generateSessionId();
        session.rotate(
                hashSessionId(rotatedSessionId),
                now,
                now.plus(GUEST_REFRESH_TOKEN_IDLE_EXPIRATION_TIME.getValue(), ChronoUnit.MILLIS)
        );
        guestBrowserSessionRepository.save(session);
        return toIssuedSession(session, rotatedSessionId);
    }

    @Transactional
    public GuestBrowserSessionIdentityDto resolveAndTouch(String sessionId) {
        Instant now = Instant.now();
        GuestBrowserSession session = findUsableSession(sessionId);
        session.touch(
                now,
                now.plus(GUEST_REFRESH_TOKEN_IDLE_EXPIRATION_TIME.getValue(), ChronoUnit.MILLIS)
        );
        guestBrowserSessionRepository.save(session);
        return GuestBrowserSessionIdentityDto.builder()
                .userId(session.getUser().getId())
                .uuid(session.getUser().getUuid())
                .username(session.getUser().getUsername())
                .build();
    }

    @Transactional
    public Optional<String> revoke(String sessionId) {
        if (!isValidSessionId(sessionId)) {
            return Optional.empty();
        }
        Optional<GuestBrowserSession> session = guestBrowserSessionRepository
                .findActiveBySessionIdHashForUpdate(hashSessionId(sessionId));
        session.ifPresent(value -> value.revoke(Instant.now()));
        return session.map(value -> String.valueOf(value.getUser().getId()));
    }

    private GuestBrowserSession findUsableSession(String sessionId) {
        if (!isValidSessionId(sessionId)) {
            throw new CustomException(ErrorCode.GUEST_SESSION_UNAUTHORIZED);
        }
        GuestBrowserSession session = guestBrowserSessionRepository
                .findActiveBySessionIdHashForUpdate(hashSessionId(sessionId))
                .orElseThrow(() -> new CustomException(ErrorCode.GUEST_SESSION_UNAUTHORIZED));
        if (session.isExpiredAt(Instant.now())) {
            throw new CustomException(ErrorCode.GUEST_SESSION_UNAUTHORIZED);
        }
        return session;
    }

    private boolean isValidSessionId(String sessionId) {
        return sessionId != null && sessionId.matches("[A-Za-z0-9_-]{43}");
    }

    private IssuedSession toIssuedSession(GuestBrowserSession session, String sessionId) {
        return IssuedSession.builder()
                .sessionId(sessionId)
                .userId(session.getUser().getId())
                .uuid(session.getUser().getUuid())
                .response(GuestBrowserSessionResponseDto.builder()
                        .expiresAt(session.getExpiresAt())
                        .absoluteExpiresAt(session.getAbsoluteExpiresAt())
                        .build())
                .build();
    }

    private String generateSessionId() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashSessionId(String sessionId) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(sessionId.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    @Getter
    @Builder
    public static class IssuedSession {
        private String sessionId;
        private Long userId;
        private String uuid;
        private GuestBrowserSessionResponseDto response;
    }
}
