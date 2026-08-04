package com.wemeetnow.auth_service.service.impl;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
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

    private final SendGrid sendGrid;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Value("${sendgrid.from-name}")
    private String fromName;

    // Redis 대신 메모리에 인증 정보를 저장할 상자 (스레드 안전)
    private final Map<String, EmailVerification> verificationStorage = new ConcurrentHashMap<>();

    @Override
    public void sendVerificationEmail(String toEmail) {
        // 1. 6자리 랜덤 인증번호 생성
        String verificationCode = generateRandomCode();
        log.info("Generated verification code {} for email {}", verificationCode, toEmail);
        
        // 2. 메모리(Map)에 저장 (이미 존재하면 덮어쓰기 되며 시간도 새로 갱신됨)
        EmailVerification verification = new EmailVerification(toEmail, verificationCode);
        verificationStorage.put(toEmail, verification);

        // 3. SendGrid로 이메일 발송 (HTTP API 사용, SMTP 포트 차단 문제 없음)
        try {
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(toEmail);
            String subject = "[위밋톡] 이메일 인증번호 안내";
            
            String emailBody = "안녕하세요. 위밋톡(WeMeetTalk)입니다.\n\n" +
                    "이메일 인증 번호 6자리는 다음과 같습니다:\n" +
                    "아래 번호를 입력하여 회원가입 절차를 완료해주세요.\n" +
                    "[" + verificationCode + "]\n\n" +
                    "본 인증 번호는 5분 후에 만료되므로, 시간 내에 인증번호 입력을 부탁드립니다.\n" +
                    "감사합니다.\n위밋톡 드림";
            
            Content content = new Content("text/plain", emailBody);
            Mail mail = new Mail(from, subject, to, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            log.info("Sending verification email to {} with code {} via SendGrid", toEmail, verificationCode);
            Response response = sendGrid.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Successfully sent verification email to {} (Status: {})", toEmail, response.getStatusCode());
            } else {
                log.error("Failed to send email. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to send email via SendGrid");
            }
            
        } catch (IOException e) {
            log.error("Error sending email via SendGrid to {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
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