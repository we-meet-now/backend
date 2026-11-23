package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.dto.CommonApiResponse;
import com.wemeetnow.auth_service.dto.GuestBrowserSessionIdentityDto;
import com.wemeetnow.auth_service.dto.GuestBrowserSessionResponseDto;
import com.wemeetnow.auth_service.domain.enums.ErrorCode;
import com.wemeetnow.auth_service.exception.CustomException;
import com.wemeetnow.auth_service.service.GuestBrowserSessionService;
import com.wemeetnow.auth_service.service.GuestBrowserSessionService.IssuedSession;
import com.wemeetnow.auth_service.service.UserLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/v1/users/no-login/guest-browser-sessions")
public class GuestBrowserSessionController {
    public static final String COOKIE_NAME = "guest_session";
    private static final String CREATE_ENDPOINT = "/api/auth/v1/users/no-login/guest-browser-sessions";
    private static final String REFRESH_ENDPOINT = CREATE_ENDPOINT + "/refresh";
    private static final String RESOLVE_ENDPOINT = CREATE_ENDPOINT + "/resolve";
    private static final String REVOKE_ENDPOINT = CREATE_ENDPOINT + "/revoke";
    private static final String RESOLVER_KEY_HEADER = "X-Guest-Session-Resolver-Key";

    private final GuestBrowserSessionService guestBrowserSessionService;
    private final UserLogService userLogService;

    @Value("${front.url}")
    private String frontUrl;

    @Value("${guest.browser-session.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${guest.browser-session.cookie-same-site:Lax}")
    private String cookieSameSite;

    @Value("${guest.browser-session.internal-secret:}")
    private String internalSecret;

    @Operation(
            summary = "브라우저 게스트 세션 생성",
            description = "요청 본문과 Authorization 헤더 없이 호출합니다. 비로그인 브라우저용 게스트를 생성하고 불투명한 세션 ID를 guest_session HttpOnly 쿠키로 설정합니다. 응답 본문에는 UUID, 세션 ID, 토큰이 포함되지 않습니다. 브라우저 fetch에서는 credentials: include를 사용하고, 프런트와 API는 app.example.com과 api.example.com처럼 같은 사이트의 도메인을 사용해야 합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "브라우저 게스트 세션 생성 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "{\"statusCode\":\"2000\",\"data\":{\"expiresAt\":\"<ISO-8601 timestamp>\",\"absoluteExpiresAt\":\"<ISO-8601 timestamp>\"},\"message\":\"브라우저 게스트 세션 생성 성공\"}")),
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = HttpHeaders.SET_COOKIE,
                            description = "guest_session 쿠키. HttpOnly; Secure; SameSite=Lax; Path=/ 속성으로 설정됩니다.")),
            @ApiResponse(responseCode = "401", description = "허용되지 않은 브라우저 Origin",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "게스트 세션 생성 실패",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CommonApiResponse<GuestBrowserSessionResponseDto>> create(
            HttpServletRequest request
    ) {
        AtomicReference<String> persistedUserId = new AtomicReference<>();
        return userLogService.executeWithLogging(
                request,
                persistedUserId::get,
                CREATE_ENDPOINT,
                () -> {
                    validateBrowserOrigin(request);
                    IssuedSession session = guestBrowserSessionService.create();
                    persistedUserId.set(String.valueOf(session.getUserId()));
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .header(HttpHeaders.SET_COOKIE, sessionCookie(session).toString())
                            .header(HttpHeaders.CACHE_CONTROL, "no-store")
                            .body(success(session.getResponse(), "브라우저 게스트 세션 생성 성공"));
                },
                response -> response.getStatusCode().value()
        );
    }

    @Operation(
            summary = "브라우저 게스트 세션 갱신",
            description = "요청 본문, Authorization 헤더, UUID 없이 현재 guest_session 쿠키로 호출합니다. 세션 ID를 회전하고 새 HttpOnly 쿠키를 Set-Cookie 응답 헤더로 설정합니다. 회전 전 쿠키 값은 즉시 무효화되며 절대 만료 시각은 연장되지 않습니다. 브라우저 fetch에서는 credentials: include를 설정하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "브라우저 게스트 세션 갱신 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "{\"statusCode\":\"2000\",\"data\":{\"expiresAt\":\"<ISO-8601 timestamp>\",\"absoluteExpiresAt\":\"<ISO-8601 timestamp>\"},\"message\":\"브라우저 게스트 세션 갱신 성공\"}")),
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = HttpHeaders.SET_COOKIE,
                            description = "새 guest_session 쿠키. 기존 쿠키 값은 더 이상 유효하지 않습니다.")),
            @ApiResponse(responseCode = "401", description = "쿠키가 없거나 세션이 만료·폐기되었거나 허용되지 않은 Origin",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<CommonApiResponse<GuestBrowserSessionResponseDto>> refresh(
            @Parameter(name = COOKIE_NAME, in = ParameterIn.COOKIE, description = "현재 브라우저 세션 쿠키", required = true)
            @CookieValue(name = COOKIE_NAME, required = false) String sessionId,
            HttpServletRequest request
    ) {
        AtomicReference<String> persistedUserId = new AtomicReference<>();
        return userLogService.executeWithLogging(
                request,
                persistedUserId::get,
                REFRESH_ENDPOINT,
                () -> {
                    validateBrowserOrigin(request);
                    IssuedSession session = guestBrowserSessionService.refresh(sessionId);
                    persistedUserId.set(String.valueOf(session.getUserId()));
                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, sessionCookie(session).toString())
                            .header(HttpHeaders.CACHE_CONTROL, "no-store")
                            .body(success(session.getResponse(), "브라우저 게스트 세션 갱신 성공"));
                },
                response -> response.getStatusCode().value()
        );
    }

    @Hidden
    @PostMapping("/resolve")
    public ResponseEntity<CommonApiResponse<GuestBrowserSessionIdentityDto>> resolve(
            @Parameter(name = COOKIE_NAME, in = ParameterIn.COOKIE, description = "검증할 브라우저 세션 쿠키", required = true)
            @CookieValue(name = COOKIE_NAME, required = false) String sessionId,
            @RequestHeader(value = RESOLVER_KEY_HEADER, required = false) String resolverKey,
            HttpServletRequest request
    ) {
        AtomicReference<String> persistedUserId = new AtomicReference<>();
        return userLogService.executeWithLogging(
                request,
                persistedUserId::get,
                RESOLVE_ENDPOINT,
                () -> {
                    validateResolverKey(resolverKey);
                    GuestBrowserSessionIdentityDto identity =
                            guestBrowserSessionService.resolveAndTouch(sessionId);
                    persistedUserId.set(String.valueOf(identity.getUserId()));
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CACHE_CONTROL, "no-store")
                            .body(success(identity, "브라우저 게스트 세션 확인 성공"));
                },
                response -> response.getStatusCode().value()
        );
    }

    @Operation(
            summary = "브라우저 게스트 세션 폐기",
            description = "요청 본문과 Authorization 헤더 없이 현재 guest_session 쿠키를 폐기합니다. 브라우저 쿠키를 만료시키는 Set-Cookie 응답을 반환합니다. 이미 없거나 폐기된 세션도 성공으로 처리합니다. 브라우저 fetch에서는 credentials: include를 설정하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "브라우저 게스트 세션 폐기 완료",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "{\"statusCode\":\"2000\",\"data\":null,\"message\":\"브라우저 게스트 세션 폐기 완료\"}")),
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = HttpHeaders.SET_COOKIE,
                            description = "만료 처리된 guest_session 쿠키")),
            @ApiResponse(responseCode = "401", description = "허용되지 않은 브라우저 Origin",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping("/revoke")
    public ResponseEntity<CommonApiResponse<Void>> revoke(
            @Parameter(name = COOKIE_NAME, in = ParameterIn.COOKIE, description = "폐기할 브라우저 세션 쿠키")
            @CookieValue(name = COOKIE_NAME, required = false) String sessionId,
            HttpServletRequest request
    ) {
        AtomicReference<String> persistedUserId = new AtomicReference<>();
        return userLogService.executeWithLogging(
                request,
                persistedUserId::get,
                REVOKE_ENDPOINT,
                () -> {
                    validateBrowserOrigin(request);
                    persistedUserId.set(guestBrowserSessionService.revoke(sessionId).orElse(null));
                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, clearSessionCookie().toString())
                            .header(HttpHeaders.CACHE_CONTROL, "no-store")
                            .body(CommonApiResponse.<Void>builder()
                                    .statusCode("2000")
                                    .data(null)
                                    .message("브라우저 게스트 세션 폐기 완료")
                                    .build());
                },
                response -> response.getStatusCode().value()
        );
    }

    private ResponseCookie sessionCookie(IssuedSession session) {
        long remainingSeconds = Math.max(
                Duration.between(Instant.now(), session.getResponse().getAbsoluteExpiresAt()).getSeconds(),
                0L
        );
        return ResponseCookie.from(COOKIE_NAME, session.getSessionId())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofSeconds(remainingSeconds))
                .build();
    }

    private ResponseCookie clearSessionCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private void validateBrowserOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank()) {
            return;
        }

        boolean allowed = Arrays.stream(frontUrl.split(","))
                .map(String::trim)
                .anyMatch(origin::equals)
                || "http://localhost:5173".equals(origin);
        if (!allowed) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateResolverKey(String resolverKey) {
        if (internalSecret == null || internalSecret.length() < 32) {
            throw new IllegalStateException("게스트 브라우저 세션 내부 인증 키가 설정되지 않았습니다.");
        }
        if (resolverKey == null || resolverKey.isBlank()
                || !MessageDigest.isEqual(internalSecret.getBytes(StandardCharsets.UTF_8),
                        resolverKey.getBytes(StandardCharsets.UTF_8))) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    private <T> CommonApiResponse<T> success(T data, String message) {
        return CommonApiResponse.<T>builder()
                .statusCode("2000")
                .data(data)
                .message(message)
                .build();
    }
}
