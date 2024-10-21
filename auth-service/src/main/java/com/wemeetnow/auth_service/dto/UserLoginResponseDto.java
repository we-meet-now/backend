package com.wemeetnow.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor @AllArgsConstructor
public class UserLoginResponseDto {
    private String accessToken;
    private String refreshToken;
}
