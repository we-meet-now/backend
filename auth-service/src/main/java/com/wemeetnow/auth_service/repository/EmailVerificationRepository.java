package com.wemeetnow.auth_service.repository;

import com.wemeetnow.auth_service.domain.EmailVerification;
import org.springframework.data.repository.CrudRepository;

public interface EmailVerificationRepository extends CrudRepository<EmailVerification, String> {
}