package com.wemeetnow.auth_service.exception;

import com.wemeetnow.auth_service.dto.CommonApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice // 모든 컨트롤러의 예외를 감시
public class GlobalExceptionHandler {

    // 1. 예상치 못한 서버 내부 에러 (500) 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonApiResponse<Void>> handleAllException(Exception e) {
        log.error("서버 내부 오류 발생: ", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                CommonApiResponse.<Void>builder()
                        .statusCode("5000")
                        .data(null)
                        .message("서버 오류가 발생했습니다. 관리자에게 문의하세요.")
                        .build()
        );
    }

    // 2. 비즈니스 로직 상 발생한 예외 처리 (예: 이메일 중복, 비밀번호 불일치 등)
    // 필요에 따라 CustomException을 만들어 처리하면 더 좋습니다.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("비즈니스 로직 에러: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                CommonApiResponse.<Void>builder()
                        .statusCode("4000")
                        .data(null)
                        .message(e.getMessage()) // "이미 존재하는 이메일입니다" 등
                        .build()
        );
    }
}