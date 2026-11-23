package com.wemeetnow.auth_service.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "guest_session", indexes = {
        @Index(name = "idx_guest_session_user_id", columnList = "user_id")
})
public class GuestSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "absolute_expires_at", nullable = false)
    private Instant absoluteExpiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public GuestSession(
            User user,
            String refreshTokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant absoluteExpiresAt
    ) {
        this.user = user;
        this.refreshTokenHash = refreshTokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.absoluteExpiresAt = absoluteExpiresAt;
    }

    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now) || !absoluteExpiresAt.isAfter(now);
    }

    public void rotate(String newRefreshTokenHash, Instant now, Instant newExpiresAt) {
        this.refreshTokenHash = newRefreshTokenHash;
        this.lastUsedAt = now;
        this.expiresAt = newExpiresAt.isBefore(absoluteExpiresAt) ? newExpiresAt : absoluteExpiresAt;
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }
}
