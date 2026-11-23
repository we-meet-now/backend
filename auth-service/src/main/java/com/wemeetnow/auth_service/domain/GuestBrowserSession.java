package com.wemeetnow.auth_service.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "guest_browser_session", indexes = {
        @Index(name = "idx_guest_browser_session_user_id", columnList = "user_id")
})
public class GuestBrowserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_id_hash", nullable = false, unique = true, length = 64)
    private String sessionIdHash;

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

    public GuestBrowserSession(
            User user,
            String sessionIdHash,
            Instant createdAt,
            Instant expiresAt,
            Instant absoluteExpiresAt
    ) {
        this.user = user;
        this.sessionIdHash = sessionIdHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.absoluteExpiresAt = absoluteExpiresAt;
    }

    public boolean isExpiredAt(Instant now) {
        return revokedAt != null || !expiresAt.isAfter(now) || !absoluteExpiresAt.isAfter(now);
    }

    public void touch(Instant now, Instant newExpiresAt) {
        this.lastUsedAt = now;
        this.expiresAt = newExpiresAt.isBefore(absoluteExpiresAt) ? newExpiresAt : absoluteExpiresAt;
    }

    public void rotate(String newSessionIdHash, Instant now, Instant newExpiresAt) {
        this.sessionIdHash = newSessionIdHash;
        touch(now, newExpiresAt);
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }
}
