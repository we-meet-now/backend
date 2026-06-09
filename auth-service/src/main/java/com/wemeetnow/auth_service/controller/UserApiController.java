package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.dto.*;
import com.wemeetnow.auth_service.service.UserService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사용자 API", description = "회원가입, 로그인, 사용자 정보 조회 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserApiController {
    private final UserService userService;

    @Operation(
            summary = "회원가입",
            description = "이메일, 비밀번호, 닉네임, 이름, 역할(role)을 입력하여 회원가입합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = UserJoinResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (이메일 중복, 비밀번호 불일치 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping("/join")
    public ResponseEntity<UserJoinResponseDto> join(@RequestBody UserJoinRequestDto requestDto) {
        UserJoinResponseDto responseDto = userService.join(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하여 AccessToken과 RefreshToken을 발급받습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "로그인 성공 (AccessToken, RefreshToken 반환)",
                    content = @Content(schema = @Schema(implementation = UserLoginResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (이메일 미존재, 비밀번호 불일치 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(@RequestBody UserLoginRequestDto requestDto) {
        log.info("UserLoginRequestDto = [{}]", requestDto);
        UserLoginResponseDto responseDto = userService.login(requestDto).getBody();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseDto);
    }

    @Operation(
            summary = "로그인 상태 확인",
            description = "Authorization 헤더의 JWT 토큰 유효성을 검증하고 " +
                          "토큰에 담긴 사용자 정보(userId, email, role, 만료시간)를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유효한 토큰 (사용자 정보 반환)",
                    content = @Content(schema = @Schema(implementation = CheckLoginResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "토큰 만료 또는 유효하지 않은 토큰")
    })
    @GetMapping("/check-is-logined")
    public ResponseEntity<CommonApiResponse<CheckLoginResponseDto>> checkIsLogined(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = authorizationHeader.replace("Bearer ", "");
        if (JwtUtil.isExpired(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<CheckLoginResponseDto>builder()
                            .statusCode("4010")
                            .data(null)
                            .message("토큰이 만료되었습니다.")
                            .build()
            );
        }
        Claims claims = JwtUtil.extractAllClaims(token);
        CheckLoginResponseDto responseDto = CheckLoginResponseDto.builder()
                .userId(claims.get("userId", Long.class))
                .email(claims.get("email", String.class))
                .role(claims.get("role", String.class))
                .expiration(claims.getExpiration())
                .build();
        return ResponseEntity.ok(CommonApiResponse.<CheckLoginResponseDto>builder()
                .statusCode("2000")
                .data(responseDto)
                .message("유효한 토큰입니다.")
                .build());
    }

    @Operation(
            summary = "전체 사용자 목록 조회",
            description = "등록된 모든 사용자 목록을 반환합니다. (관리자용)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 목록 조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/all")
    public ResponseEntity<CommonApiResponse<List<User>>> getUsersAll() {
        List<User> allUsers = userService.getAllUsers();
        return ResponseEntity.ok(CommonApiResponse.<List<User>>builder()
                .statusCode("2000")
                .data(allUsers)
                .message("사용자 목록 조회 성공")
                .build());
    }

    @Operation(
            summary = "AccessToken으로 userId 조회",
            description = "Authorization 헤더의 JWT AccessToken을 파싱하여 " +
                          "사용자 ID(userId)를 반환합니다. MSA 서비스 간 내부 호출에 사용됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "userId 조회 성공",
                    content = @Content(schema = @Schema(implementation = AuthUserDto.class))),
            @ApiResponse(responseCode = "401", description = "토큰 만료 또는 유효하지 않은 토큰")
    })
    @GetMapping("/get-id")
    public ResponseEntity<AuthUserDto> getUserIdFromAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = authorizationHeader.replace("Bearer ", "");
        if (JwtUtil.isExpired(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    AuthUserDto.builder().userId(0L).statusCd("4010").statusMsg("토큰이 만료되었습니다.").build()
            );
        }
        Long userId = JwtUtil.getId(token);
        return ResponseEntity.ok(AuthUserDto.builder()
                .userId(userId)
                .statusCd("2000")
                .statusMsg("success")
                .build());
    }

    @Operation(
            summary = "AccessToken으로 사용자 상세 정보 조회",
            description = "Authorization 헤더의 JWT AccessToken으로 로그인한 사용자의 " +
                          "상세 정보(이름, 이메일, 닉네임, 프로필 이미지 등)를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatParticipantUserDto.class))),
            @ApiResponse(responseCode = "401", description = "토큰 만료 또는 유효하지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/get-user-info")
    public ResponseEntity<ChatParticipantUserDto> getUserInfoFromAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = authorizationHeader.replace("Bearer ", "");
        if (JwtUtil.isExpired(token)) {
            log.info("토큰이 유효하지 않습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = JwtUtil.getId(token);
        User findUser = userService.getUserById(userId).orElse(null);
        if (findUser == null) {
            log.info("사용자가 존재하지 않습니다. userId: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        ChatParticipantUserDto responseDto = ChatParticipantUserDto.builder()
                .userId(findUser.getId())
                .username(findUser.getUsername())
                .email(findUser.getEmail())
                .nickname(findUser.getNickname())
                .imgUrl(findUser.getImgUrl())
                .phoneNumber(findUser.getPhoneNumber())
                .build();
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "userId로 사용자 정보 조회",
            description = "userId(PK)로 특정 사용자의 상세 정보를 조회합니다. " +
                          "MSA 서비스 간 내부 호출에 사용됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatParticipantUserDto.class))),
            @ApiResponse(responseCode = "404", description = "해당 userId의 사용자를 찾을 수 없음")
    })
    @GetMapping("/userId={userId}")
    public ResponseEntity<ChatParticipantUserDto> getUserById(@PathVariable("userId") Long userId) {
        User findUser = userService.getUserById(userId).orElse(null);
        if (findUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        ChatParticipantUserDto responseDto = ChatParticipantUserDto.builder()
                .userId(findUser.getId())
                .email(findUser.getEmail())
                .nickname(findUser.getNickname())
                .imgUrl(findUser.getImgUrl())
                .phoneNumber(findUser.getPhoneNumber())
                .build();
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "랜덤 닉네임 생성",
            description = "무작위 닉네임을 생성하여 반환합니다. 회원가입 시 닉네임 추천에 사용됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "랜덤 닉네임 생성 성공",
                    content = @Content(schema = @Schema(implementation = RandomNicknameResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/create-random-nickname")
    public ResponseEntity<CommonApiResponse<RandomNicknameResponseDto>> createRandomNickname() {
        String randomNickname = userService.createRandomNickname();
        return ResponseEntity.ok(CommonApiResponse.<RandomNicknameResponseDto>builder()
                .statusCode("2000")
                .data(new RandomNicknameResponseDto(randomNickname))
                .message("랜덤 닉네임 생성 성공")
                .build());
    }
}