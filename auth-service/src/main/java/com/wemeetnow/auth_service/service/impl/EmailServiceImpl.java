package com.wemeetnow.auth_service.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import com.wemeetnow.auth_service.domain.EmailVerification;
import com.wemeetnow.auth_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    // private final SendGrid sendGrid;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Redis 대신 메모리에 인증 정보를 저장할 상자 (스레드 안전)
    private final Map<String, EmailVerification> verificationStorage = new ConcurrentHashMap<>();

    @Override
    public void sendVerificationEmail(String toEmail) {
        MimeMessage message = mailSender.createMimeMessage();
        // 1. 6자리 랜덤 인증번호 생성
        String verificationCode = generateRandomCode();
        log.info("Generated verification code {} for email {}", verificationCode, toEmail);
        
        // 2. 메모리(Map)에 저장 (이미 존재하면 덮어쓰기 되며 시간도 새로 갱신됨)
        EmailVerification verification = new EmailVerification(toEmail, verificationCode);
        verificationStorage.put(toEmail, verification);

        try {
            String subject = "[위밋톡] 이메일 인증번호 안내";

            // 모던 HTML 메일 템플릿
            String emailBody = """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; background-color: #f4f6f8; font-family: 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;">
                <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #f4f6f8; padding: 40px 10px;">
                    <tr>
                        <td align="center">
                            <!-- 메인 카드 컨테이너 -->
                            <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 500px; background-color: #ffffff; border-radius: 12px; border: 1px solid #e5e7eb; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);">
                                <!-- 상단 헤더 / 브랜드 로고 -->
                                <tr>
                                    <td style="padding: 32px 32px 20px 32px;">
                                        <span style="font-size: 22px; font-weight: 800; color: #2563eb; letter-spacing: -0.5px;">WeMeetTalk</span>
                                    </td>
                                </tr>
                                
                                <!-- 본문 콘텐츠 -->
                                <tr>
                                    <td style="padding: 0 32px 32px 32px;">
                                        <h1 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 700; color: #111827; line-height: 1.4;">이메일 인증번호 안내</h1>
                                        <p style="margin: 0 0 24px 0; font-size: 15px; color: #4b5563; line-height: 1.6;">
                                            안녕하세요. <strong>위밋톡(WeMeetTalk)</strong>입니다.<br>
                                            회원가입 절차를 완료하기 위해 아래의 인증번호를 입력해 주세요.
                                        </p>
                                        
                                        <!-- 인증번호 강조 박스 -->
                                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px; text-align: center; margin-bottom: 24px;">
                                            <span style="font-size: 32px; font-weight: 800; color: #1e293b; letter-spacing: 8px; font-family: 'Courier New', monospace;">%s</span>
                                        </div>
                                        
                                        <!-- 만료 시간 안내 -->
                                        <p style="margin: 0; font-size: 13px; color: #dc2626; line-height: 1.5; font-weight: 500;">
                                            ⏰ 본 인증번호는 <strong>5분 후</strong>에 만료됩니다.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- 구분선 -->
                                <tr>
                                    <td style="padding: 0 32px;">
                                        <div style="border-top: 1px solid #f1f5f9;"></div>
                                    </td>
                                </tr>
                                
                                <!-- 푸터 -->
                                <tr>
                                    <td style="padding: 24px 32px; background-color: #fafafa; text-align: center;">
                                        <p style="margin: 0 0 6px 0; font-size: 12px; color: #9ca3af;">
                                            본 메일은 발신 전용 메일입니다. 문의사항은 고객센터를 이용해 주세요.
                                        </p>
                                        <p style="margin: 0; font-size: 12px; color: #9ca3af; font-weight: 600;">
                                            © WeMeetTalk. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                                
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(verificationCode);

            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(emailBody, true); // false = 일반 텍스트

            mailSender.send(message);
            log.info("Email has been sent successfully to {}", toEmail);
        } catch (Exception e) {
            throw new RuntimeException("이메일 발송 중 오류 발생: " + e.getMessage(), e);
        }
    }

    // 추후 인증번호 검증 로직 구현 시 참고할 수 있는 메서드 예시
    public boolean verifyEmail(String email, String userInputCode) {
        EmailVerification verification = verificationStorage.get(email);

        if (verification == null) return false;       // 보낸 적 없음
        if (verification.isExpired()) {
            verificationStorage.remove(email);         // 만료되었으면 메모리 청소
            return false;
        }

        return verification.getVerificationCode().equals(userInputCode);
    }

    private String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}