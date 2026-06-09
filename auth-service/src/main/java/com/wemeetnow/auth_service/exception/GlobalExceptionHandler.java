package com.wemeetnow.auth_service.exception;

import com.wemeetnow.auth_service.dto.CommonApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleInvalidPassword(InvalidPasswordException ex) {
        log.error("InvalidPasswordException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                CommonApiResponse.<Void>builder()
                        .statusCode("4001")
                        .data(null)
                        .message(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                CommonApiResponse.<Void>builder()
                        .statusCode("5005")
                        .data(null)
                        .message(ex.getMessage() != null ? ex.getMessage() : "서버 오류가 발생했습니다.")
                        .build()
        );
    }
}

