package com.wemeetnow.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoLoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private String statusCd;
    private String message;
    private String nickname;     // 카카오 프로필 닉네임
}

