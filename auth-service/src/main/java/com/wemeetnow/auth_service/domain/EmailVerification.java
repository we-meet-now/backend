package com.wemeetnow.auth_service.domain;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class EmailVerification {
    private final String email;
    private final String verificationCode;
    private final LocalDateTime createdAt;

    public EmailVerification(String email, String verificationCode) {
        this.email = email;
        this.verificationCode = verificationCode;
        this.createdAt = LocalDateTime.now();
    }

    // 5분이 지났는지 확인하는 메서드 (기획에 따라 minutes(5) 등으로 변경 가능)
    public boolean isExpired() {
        return this.createdAt.plusHours(4).isBefore(LocalDateTime.now());
    }
}