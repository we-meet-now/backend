package com.wemeetnow.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private int statusCode;
    private String message;

    public UserLoginResponseDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.statusCode = 200;
        this.message = "로그인 성공";
    }

    public static UserLoginResponseDto fail(int statusCode, String message) {
        return new UserLoginResponseDto(null, null, statusCode, message);
    }
}
