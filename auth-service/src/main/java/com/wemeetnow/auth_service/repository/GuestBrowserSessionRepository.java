package com.wemeetnow.auth_service.repository;

import com.wemeetnow.auth_service.domain.GuestBrowserSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuestBrowserSessionRepository extends JpaRepository<GuestBrowserSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from GuestBrowserSession session where session.sessionIdHash = :sessionIdHash and session.revokedAt is null")
    Optional<GuestBrowserSession> findActiveBySessionIdHashForUpdate(
            @Param("sessionIdHash") String sessionIdHash
    );
}
