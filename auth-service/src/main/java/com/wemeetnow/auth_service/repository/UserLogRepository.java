package com.wemeetnow.auth_service.repository;

import com.wemeetnow.auth_service.domain.UserLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLogRepository extends JpaRepository<UserLog, Long> {
}
