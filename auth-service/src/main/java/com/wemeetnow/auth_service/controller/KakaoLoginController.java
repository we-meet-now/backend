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
                KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(kakaoAccessToken);
                for (String key : userInfo.getProperties().keySet()) {
                    log.info("userInfo.get({}): {}", key, userInfo.getProperties().get(key));
                    body.put(key, userInfo.getProperties().get(key));
                }
                // User findUser = kakaoService.getUserFromAuthID(userInfo.getId());
                User findUser = kakaoService.findByEmail(userInfo.getKakaoAccount().getEmail());
                Role kakaoUserRole = Role.valueOf("ROLE_USER");
                String statusCd = "";
                String msg = "";
                // NOTE isEmailVerified (이메일 인증 여부) 등의 조건을 비교해서 validation 로직 추가하기
                // NOTE: AUTH_ID 로 찾기 -> AUTH_ID를 key로 두기 -> 테이블 변경 및 저장로직 변경 필요
                if (findUser == null) { // 신규 사용자인 경우(회원가입 진행)
                    UserJoinResponseDto joinResponseDto = userService.join(UserJoinRequestDto.fromKakaoDto(userInfo, kakaoUserRole));
                    findUser = userService.getUserByEmail(joinResponseDto.getEmail());
                    statusCd = "2100";
                    msg = "회원가입에 성공했습니다.";
                } else { // 이미 가입된 사용지 인 경우(로그인 진행)
                    statusCd = "2000";
                    msg = "로그인에 성공했습니다.";
                }
                String customAccessToken = jwtUtil.generateAccessToken(findUser.getId(), findUser.getEmail(), findUser.getRole());
                String customRefreshToken = jwtUtil.generateRefreshToken(findUser.getId(), findUser.getEmail(), findUser.getRole());
                body.put("accessToken", customAccessToken);
                body.put("refreshToken", customRefreshToken);
                body.put("statusCd", statusCd);
                body.put("msg", msg);
            }
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(body);
    }
}
