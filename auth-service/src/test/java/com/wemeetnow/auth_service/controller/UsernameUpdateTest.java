package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.ErrorCode;
import com.wemeetnow.auth_service.dto.CommonApiResponse;
import com.wemeetnow.auth_service.dto.SetUsernameRequestDto;
import com.wemeetnow.auth_service.dto.SetUsernameResponseDto;
import com.wemeetnow.auth_service.exception.CustomException;
import com.wemeetnow.auth_service.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UsernameUpdateTest {

    private static final String SECRET = "01234567890123456789012345678901";

    private Object originalSecret;
    private UserService userService;
    private JwtUtil jwtUtil;
    private UserApiController controller;

    @BeforeEach
    void setUp() {
        originalSecret = ReflectionTestUtils.getField(JwtUtil.class, "SECRET_KEY");
        ReflectionTestUtils.setField(JwtUtil.class, "SECRET_KEY", SECRET);
        userService = mock(UserService.class);
        jwtUtil = mock(JwtUtil.class);
        controller = new UserApiController(userService, jwtUtil);
    }

    @AfterEach
    void restoreSecret() {
        ReflectionTestUtils.setField(JwtUtil.class, "SECRET_KEY", originalSecret);
    }

    @Test
    void updatesUsernameForAccessTokenUserId() {
        String token = createToken(13L, null);
        User user = User.builder().username("before").build();
        when(jwtUtil.getAuthenticatedUser(org.mockito.ArgumentMatchers.any())).thenReturn(user);
        when(userService.updateUsername(user, "푹신한사막여우873"))
                .thenReturn(new SetUsernameResponseDto(true, "푹신한사막여우873"));

        ResponseEntity<CommonApiResponse<SetUsernameResponseDto>> response =
                controller.setUsername("Bearer " + token, request("Bearer " + token), requestDto("푹신한사막여우873"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatusCode()).isEqualTo("2000");
        assertThat(response.getBody().getData().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getUsername()).isEqualTo("푹신한사막여우873");
        verify(userService).updateUsername(user, "푹신한사막여우873");
    }

    @Test
    void updatesUsernameForGuestTokenUuid() {
        String uuid = "guest-uuid";
        String token = createToken(null, uuid);
        User user = User.builder().username("before").build();
        when(userService.getUserByUUID(uuid)).thenReturn(Optional.of(user));
        when(userService.updateUsername(user, "guest-name"))
                .thenReturn(new SetUsernameResponseDto(true, "guest-name"));

        ResponseEntity<CommonApiResponse<SetUsernameResponseDto>> response =
                controller.setUsername("Bearer " + token, request("Bearer " + token), requestDto("guest-name"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getUsername()).isEqualTo("guest-name");
        verify(userService).getUserByUUID(uuid);
        verify(userService).updateUsername(user, "guest-name");
    }

    @Test
    void rejectsExpiredToken() {
        String token = createToken(13L, null, Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> controller.setUsername(
                "Bearer " + token, request("Bearer " + token), requestDto("new-name")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verifyNoInteractions(userService, jwtUtil);
    }

    @Test
    void rejectsGuestTokenWhenUserDoesNotExist() {
        String uuid = "missing-guest-uuid";
        String token = createToken(null, uuid);
        when(userService.getUserByUUID(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.setUsername(
                "Bearer " + token, request("Bearer " + token), requestDto("new-name")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userService).getUserByUUID(uuid);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void rejectsMissingBearerToken() {
        assertThatThrownBy(() -> controller.setUsername(null, request(null), requestDto("new-name")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verifyNoInteractions(userService, jwtUtil);
    }

    @Test
    void rejectsBlankUsername() {
        assertThatThrownBy(() -> controller.setUsername(
                "Bearer token", request("Bearer token"), requestDto("  ")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verifyNoInteractions(userService, jwtUtil);
    }

    private String createToken(Long userId, String uuid) {
        return createToken(userId, uuid, Instant.now().plusSeconds(600));
    }

    private String createToken(Long userId, String uuid, Instant expiration) {
        var builder = Jwts.builder();
        if (userId != null) {
            builder.claim("userId", userId);
        }
        if (uuid != null) {
            builder.claim("uuId", uuid);
        }
        return builder.setExpiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private MockHttpServletRequest request(String authorizationHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorizationHeader != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return request;
    }

    private SetUsernameRequestDto requestDto(String username) {
        SetUsernameRequestDto requestDto = new SetUsernameRequestDto();
        requestDto.setUsername(username);
        return requestDto;
    }
}
