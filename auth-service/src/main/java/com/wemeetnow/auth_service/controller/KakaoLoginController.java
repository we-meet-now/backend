package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.Role;
import com.wemeetnow.auth_service.dto.*;
import com.wemeetnow.auth_service.service.KakaoService;
import com.wemeetnow.auth_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "카카오 로그인 API", description = "카카오 OAuth2 로그인 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("")
public class KakaoLoginController {

    private final KakaoService kakaoService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(
            summary = "카카오 로그인 콜백",
            description = "카카오 OAuth2 인증 후 리다이렉트되는 콜백 엔드포인트입니다.\n\n" +
                          "- 신규 사용자: 자동 회원가입 후 AccessToken/RefreshToken 발급 (statusCd: 2100)\n" +
                          "- 기존 사용자: 로그인 처리 후 AccessToken/RefreshToken 발급 (statusCd: 2000)\n" +
                          "- 카카오로부터 받은 인가코드(code)로 카카오 AccessToken을 발급받고, " +
                          "사용자 정보를 조회하여 자체 JWT를 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인/회원가입 성공 (AccessToken, RefreshToken 반환)",
                    content = @Content(schema = @Schema(implementation = KakaoLoginResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "카카오 인가코드 오류 또는 사용자 정보 조회 실패",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/callback")
    public ResponseEntity<CommonApiResponse<KakaoLoginResponseDto>> callback(
            @Parameter(description = "카카오 인가 코드 (카카오 OAuth2 서버에서 전달)", required = true)
            @RequestParam("code") String code) {

        log.info("카카오 콜백 code: {}", code);

        String kakaoAccessToken = kakaoService.getAccessTokenFromKakao(code);
        KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(kakaoAccessToken);

        User findUser = kakaoService.findByEmail(userInfo.getKakaoAccount().getEmail());
        Role kakaoUserRole = Role.ROLE_USER;
        String statusCd;
        String message;

        if (findUser == null) { // 신규 사용자 → 자동 회원가입
            UserJoinResponseDto joinResponseDto = userService.join(
                    UserJoinRequestDto.fromKakaoDto(userInfo, kakaoUserRole));
            findUser = userService.getUserByEmail(joinResponseDto.getEmail());
            statusCd = "2100";
            message = "회원가입에 성공했습니다.";
            log.info("카카오 신규 가입 완료 email: {}", joinResponseDto.getEmail());
        } else { // 기존 사용자 → 로그인
            statusCd = "2000";
            message = "로그인에 성공했습니다.";
            log.info("카카오 로그인 완료 email: {}", findUser.getEmail());
        }

        String accessToken = jwtUtil.generateAccessToken(findUser.getId(), findUser.getEmail(), findUser.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(findUser.getId(), findUser.getEmail(), findUser.getRole());

        String nickname = userInfo.getProperties() != null
                ? userInfo.getProperties().getOrDefault("nickname", "")
                : "";

        KakaoLoginResponseDto responseDto = KakaoLoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .statusCd(statusCd)
                .message(message)
                .nickname(nickname)
                .build();

        return ResponseEntity.ok(CommonApiResponse.<KakaoLoginResponseDto>builder()
                .statusCode(statusCd)
                .data(responseDto)
                .message(message)
                .build());
    }
}
