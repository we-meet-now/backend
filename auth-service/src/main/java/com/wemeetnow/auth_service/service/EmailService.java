package com.wemeetnow.auth_service.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail);
    boolean verifyEmail(String email, String verificationCode);
}