package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.Role;
import com.wemeetnow.auth_service.dto.KakaoUserInfoResponseDto;
import com.wemeetnow.auth_service.dto.UserJoinRequestDto;
import com.wemeetnow.auth_service.dto.UserJoinResponseDto;
import com.wemeetnow.auth_service.service.KakaoService;
import com.wemeetnow.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("")
public class KakaoLoginController {

    private final KakaoService kakaoService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

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
                String kakaoAccessToken = kakaoService.getAccessTokenFromKakao(code);
                body.put("kakaoAccessToken", kakaoAccessToken);
                KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(kakaoAccessToken);
                for (String key : userInfo.getProperties().keySet()) {
                    log.info("userInfo.get({}): {}", key, userInfo.getProperties().get(key));
                }
                User findUser = kakaoService.getUserFromAuthID(userInfo.getId());
                Role kakaoUserRole = Role.valueOf("ROLE_USER");
                Long userId = 0L;
                // NOTE isEmailVerified (이메일 인증 여부) 등의 조건을 비교해서 validation 로직 추가하기
                if (findUser == null) { // 신규 사용자인 경우(회원가입 진행)
                    UserJoinResponseDto joinResponseDto = userService.join(UserJoinRequestDto.fromKakaoDto(userInfo, kakaoUserRole));
                    userId = joinResponseDto.getUserId();
                } else { // 이미 가입된 사용지 인 경우(로그인 진행)
                    userId = findUser.getId();
                }
                String customAccessToken = jwtUtil.generateAccessToken(userId, findUser.getEmail(), findUser.getRole());
                String customRefreshToken = jwtUtil.generateRefreshToken(userId, findUser.getEmail(), findUser.getRole());
                body.put("accessToken", customAccessToken);
                body.put("refreshToken", customRefreshToken);
            }
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(body);
    }
}
