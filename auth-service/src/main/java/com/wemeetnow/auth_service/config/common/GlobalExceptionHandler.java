package com.wemeetnow.auth_service.config.common;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.wemeetnow.auth_service.dto.CommonApiResponse;
import com.wemeetnow.auth_service.dto.StoreRecommendRequestDto;
import com.wemeetnow.auth_service.exception.InvalidPasswordException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.reflect.Field;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * [수정] JSON 파싱 에러 응답 규격을 CommonApiResponse로 통일
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleJsonParsingException(HttpMessageNotReadableException ex) {
        log.error("JSON Parsing Error: {}", ex.getMessage());

        String fieldName = "알 수 없는 필드";
        String expectedType = "올바른 데이터 형식을 사용하세요.";

        if (ex.getCause() instanceof JsonMappingException) {
            JsonMappingException jsonEx = (JsonMappingException) ex.getCause();
            if (!jsonEx.getPath().isEmpty()) {
                fieldName = jsonEx.getPath().get(0).getFieldName();
                expectedType = getExpectedType(StoreRecommendRequestDto.class, fieldName);
            }
        }

        String errorMessage = String.format("%s 필드의 데이터 타입이 올바르지 않습니다. 기대되는 타입: %s", fieldName, expectedType);

        return ResponseEntity.badRequest().body(
                CommonApiResponse.<Void>builder()
                        .statusCode("4000") // JSON 포맷 에러 공통 코드
                        .data(null)
                        .message(errorMessage)
                        .build()
        );
    }

    /**
     * DTO 클래스에서 특정 필드의 기대 데이터 타입을 찾아 반환하는 메서드
     */
    private String getExpectedType(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            Class<?> fieldType = field.getType();

            if (fieldType.equals(String.class)) {
                return "문자열(String). 예: \"37.514229\"";
            } else if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
                return "숫자(int). 예: 25";
            } else if (fieldType.equals(List.class)) {
                return "배열(List). 예: [\"친구모임\"]";
            } else {
                return "알 수 없는 타입";
            }
        } catch (NoSuchFieldException e) {
            return "알 수 없는 필드";
        }
    }

    /**
     * 비밀번호 불일치 예외 처리
     */
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

    /**
     * [추가] 비즈니스 로직 예외 처리 (예: 중복 이메일, 존재하지 않는 계정 등)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                CommonApiResponse.<Void>builder()
                        .statusCode("4002") // 비즈니스 요구사항 위반 코드
                        .data(null)
                        .message(ex.getMessage())
                        .build()
        );
    }

    /**
     * 예상치 못한 최상위 시스템 예외 처리
     */
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