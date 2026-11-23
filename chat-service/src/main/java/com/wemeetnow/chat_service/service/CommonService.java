package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.dto.AuthUserDto;
import com.wemeetnow.chat_service.dto.CommonApiResponse;
import com.wemeetnow.chat_service.dto.GuestBrowserSessionDto;
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

    @Value("${guest.browser-session.internal-secret:}")
    private String guestBrowserSessionInternalSecret;

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

    public GuestBrowserSessionDto fetchGuestBrowserSessionFromAuthService(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("게스트 브라우저 세션 쿠키가 필요합니다.");
        }
        if (guestBrowserSessionInternalSecret == null || guestBrowserSessionInternalSecret.length() < 32) {
            throw new IllegalStateException("게스트 브라우저 세션 내부 인증 키가 설정되지 않았습니다.");
        }

        CommonApiResponse<GuestBrowserSessionDto> response = restClientBuilder
                .baseUrl(authServiceUrl)
                .build()
                .post()
                .uri("/api/auth/v1/users/no-login/guest-browser-sessions/resolve")
                .header(HttpHeaders.COOKIE, "guest_session=" + sessionId)
                .header("X-Guest-Session-Resolver-Key", guestBrowserSessionInternalSecret)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<CommonApiResponse<GuestBrowserSessionDto>>() {});

        GuestBrowserSessionDto guest = requireSuccessfulResponse(
                response,
                "Auth 서비스에서 유효한 브라우저 게스트 세션을 받지 못했습니다.");
        if (guest.getUserId() == null || guest.getUuid() == null || guest.getUuid().isBlank()) {
            throw new IllegalStateException("Auth 서비스 응답에 유효한 브라우저 게스트 ID가 없습니다.");
        }
        return guest;
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
