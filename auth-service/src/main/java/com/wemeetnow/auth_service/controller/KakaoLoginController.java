package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.dto.KakaoUserInfoResponseDto;
import com.wemeetnow.auth_service.service.KakaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("")
public class KakaoLoginController {

    private final KakaoService kakaoService;

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam("code") String code) {
        // NOTE 카카오로부터 받은 code를 카카오에 토큰발급 요청하면 사용자 정보가 담겨져있는 토큰 받을 수 있다
        // 즉, https://kauth.kakao.com/oauth/token URL로 POST 요청을 보내면, 토큰을 받을 수 있다.
        Map<String, String> body = new HashMap();
        HttpStatus status = HttpStatus.OK;
        try {
            body.put("code", code);
            log.info("code: {}", code);
            if(code != null || !code.isEmpty() || !code.isBlank()) {
                String accessToken = kakaoService.getAccessTokenFromKakao(code);
                body.put("accessToken", accessToken);
                KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(accessToken);
                for (String key : userInfo.getProperties().keySet()) {
                    log.info("userInfo.get({}): {}", key, userInfo.getProperties().get(key));
                }
            }
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(body);
    }
}
