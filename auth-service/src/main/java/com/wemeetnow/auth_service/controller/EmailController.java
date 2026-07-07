package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.dto.EmailRequestDto;
import com.wemeetnow.auth_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/v1/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequestDto requestDto) {
        emailService.sendVerificationEmail(requestDto.getEmail());
        return ResponseEntity.ok("인증 이메일이 발송되었습니다. (5분 동안 유효)");
    }
}