package com.wemeetnow.auth_service.config.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public enum JwtExpirationEnums {
    ACCESS_TOKEN_EXPIRATION_TIME("JWT 만료 시간 / 30분", 1000L * 60 * 30),
    REFRESH_TOKEN_EXPIRATION_TIME("Refresh 토큰 만료 시간 / 7일", 1000L * 60 * 60 * 24 * 7),
    GUEST_ACCESS_TOKEN_EXPIRATION_TIME("게스트 AccessToken 만료 시간 / 30분", 1000L * 60 * 30),
    GUEST_REFRESH_TOKEN_IDLE_EXPIRATION_TIME("게스트 refresh token 유휴 만료 시간 / 30일", 1000L * 60 * 60 * 24 * 30),
    GUEST_REFRESH_TOKEN_ABSOLUTE_EXPIRATION_TIME("게스트 refresh token 절대 만료 시간 / 180일", 1000L * 60 * 60 * 24 * 180),
    REISSUE_EXPIRATION_TIME("Refresh 토큰 만료 시간 / 3일", 1000L * 60 * 60 * 24 * 3);

    private String description;
    private Long value;
}
