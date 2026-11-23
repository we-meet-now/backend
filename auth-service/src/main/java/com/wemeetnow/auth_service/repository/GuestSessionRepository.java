package com.wemeetnow.auth_service.repository;

import com.wemeetnow.auth_service.domain.GuestSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuestSessionRepository extends JpaRepository<GuestSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from GuestSession session where session.refreshTokenHash = :tokenHash and session.revokedAt is null")
    Optional<GuestSession> findActiveByRefreshTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
