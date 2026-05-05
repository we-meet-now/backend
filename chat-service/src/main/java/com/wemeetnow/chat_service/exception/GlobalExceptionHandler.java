package com.wemeetnow.chat_service.exception;

import com.wemeetnow.chat_service.dto.CommonApiResponse;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // springdoc(Swagger) 내부 동작 중 발생하는 ServletException은 처리하지 않음 (재throw)
    @ExceptionHandler(ServletException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleServletException(ServletException ex) throws ServletException {
        // Swagger /v3/api-docs 요청 시 발생하는 내부 에러는 Spring에 위임
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        CommonApiResponse<Void> response = CommonApiResponse.<Void>builder()
                .statusCode("5005")
                .data(null)
                .message(ex.getMessage() != null ? ex.getMessage() : "서버 오류가 발생했습니다.")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
