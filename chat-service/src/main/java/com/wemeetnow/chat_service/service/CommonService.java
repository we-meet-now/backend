package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.dto.AuthUserDto;
import com.wemeetnow.chat_service.dto.CommonApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class CommonService {

    private final RestClient.Builder restClientBuilder;

    @Value("${external.auth-service.url}")
    private String authServiceUrl;

    public AuthUserDto fetchUserFromAuthService(String token) {
        CommonApiResponse<AuthUserDto> response = restClientBuilder
                .baseUrl(authServiceUrl)
                .build()
                .get()
                .uri("/api/auth/v1/users/get-id")
                .header(HttpHeaders.AUTHORIZATION, toBearerToken(token))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<CommonApiResponse<AuthUserDto>>() {});

        AuthUserDto user = requireSuccessfulResponse(response, "Auth 서비스에서 사용자 정보를 받지 못했습니다.");
        if (user.getUserId() == null || !ApiStatusCode.SUCCESS.getCode().equals(user.getStatusCd())) {
            throw new IllegalStateException("Auth 서비스 응답에 유효한 사용자 ID가 없습니다.");
        }
        return user;
    }

    public ChatUserInfo fetchUserInfoFromAuthService(String token) {
        CommonApiResponse<ChatUserInfo> response = restClientBuilder
                .baseUrl(authServiceUrl)
                .build()
                .get()
                .uri("/api/auth/v1/users/get-user-info")
                .header(HttpHeaders.AUTHORIZATION, toBearerToken(token))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<CommonApiResponse<ChatUserInfo>>() {});

        ChatUserInfo userInfo = requireSuccessfulResponse(response, "Auth 서비스에서 사용자 상세 정보를 받지 못했습니다.");
        if (userInfo.getUserId() == null) {
            throw new IllegalStateException("Auth 서비스 응답에 사용자 ID가 없습니다.");
        }
        return userInfo;
    }

    private <T> T requireSuccessfulResponse(CommonApiResponse<T> response, String errorMessage) {
        if (response == null
                || !ApiStatusCode.SUCCESS.getCode().equals(response.getStatusCode())
                || response.getData() == null) {
            throw new IllegalStateException(errorMessage);
        }
        return response.getData();
    }

    private String toBearerToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Authorization 토큰이 필요합니다.");
        }
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }
}
