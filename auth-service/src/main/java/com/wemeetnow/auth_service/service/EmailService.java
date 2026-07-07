package com.wemeetnow.auth_service.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail);
}