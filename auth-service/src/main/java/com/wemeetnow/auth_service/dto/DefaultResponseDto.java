package com.wemeetnow.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class DefaultResponseDto<T> {
    private String statusCd;
    private String message;
    private Map<String, T> body;
}
