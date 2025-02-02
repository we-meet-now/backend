package com.wemeetnow.auth_service.config.common;


import com.fasterxml.jackson.databind.JsonMappingException;
import com.wemeetnow.auth_service.dto.StoreRecommendRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonParsingException(HttpMessageNotReadableException ex) {
        log.error("JSON Parsing Error: {}", ex.getMessage());

        String fieldName = "알 수 없는 필드";
        String expectedType = "올바른 데이터 형식을 사용하세요.";

        // 예외의 원인이 JsonMappingException인지 확인
        if (ex.getCause() instanceof JsonMappingException) {
            JsonMappingException jsonEx = (JsonMappingException) ex.getCause();
            if (!jsonEx.getPath().isEmpty()) {
                fieldName = jsonEx.getPath().get(0).getFieldName(); // 예외가 발생한 필드명 가져오기

                // 필드의 기대 데이터 타입을 Reflection으로 가져오기
                expectedType = getExpectedType(StoreRecommendRequestDto.class, fieldName);
            }
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Invalid JSON Format");
        errorResponse.put("message", String.format("%s 필드의 데이터 타입이 올바르지 않습니다. 기대되는 타입: %s", fieldName, expectedType));

        return ResponseEntity.badRequest().body(errorResponse);
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
}
