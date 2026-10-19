package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.dto.CommonApiResponse;
import com.wemeetnow.auth_service.dto.GuestCheckRequestDto;
import com.wemeetnow.auth_service.dto.GuestIdRequestDto;
import com.wemeetnow.auth_service.dto.GuestYnDto;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GuestTokenClassificationTest {

    private static final String SECRET = "01234567890123456789012345678901";

    private Object originalSecret;
    private UserService userService;
    private UserApiController controller;

    @BeforeEach
    void setUp() {
        originalSecret = ReflectionTestUtils.getField(JwtUtil.class, "SECRET_KEY");
        ReflectionTestUtils.setField(JwtUtil.class, "SECRET_KEY", SECRET);
        userService = mock(UserService.class);
        controller = new UserApiController(userService, mock(JwtUtil.class));
    }

    @AfterEach
    void restoreSecret() {
        ReflectionTestUtils.setField(JwtUtil.class, "SECRET_KEY", originalSecret);
    }

    @Test
    void identifiesValidAccessTokenAsNonGuest() {
        String token = createToken(11L, null, Instant.now().plusSeconds(600));

        ResponseEntity<CommonApiResponse<GuestYnDto>> response =
                controller.getIsGuestTokenYn(request(token));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getIsGuestYn()).isEqualTo("N");
        verifyNoInteractions(userService);
    }

    @Test
    void identifiesExistingGuestTokenAsGuest() {
        String uuid = "guest-uuid";
        String token = createToken(null, uuid, Instant.now().plusSeconds(600));
        when(userService.getUserByUUID(uuid)).thenReturn(Optional.of(mock(User.class)));

        ResponseEntity<CommonApiResponse<GuestYnDto>> response =
                controller.getIsGuestTokenYn(request(token));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getIsGuestYn()).isEqualTo("Y");
        verify(userService).getUserByUUID(uuid);
    }

    @Test
    void keepsExpiredGuestTokenClassifiedAsGuestForRefresh() {
        String uuid = "expired-guest-uuid";
        String token = createToken(null, uuid, Instant.now().minusSeconds(60));
        when(userService.getUserByUUID(uuid)).thenReturn(Optional.of(mock(User.class)));

        ResponseEntity<CommonApiResponse<GuestYnDto>> response =
                controller.getIsGuestTokenYn(request(token));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getIsGuestYn()).isEqualTo("Y");
        verify(userService).getUserByUUID(uuid);
    }

    @Test
    void rejectsExpiredAccessToken() {
        String token = createToken(11L, null, Instant.now().minusSeconds(60));

        ResponseEntity<CommonApiResponse<GuestYnDto>> response =
                controller.getIsGuestTokenYn(request(token));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatusCode()).isEqualTo("4010");
        verifyNoInteractions(userService);
    }

    @Test
    void rejectsMissingToken() {
        ResponseEntity<CommonApiResponse<GuestYnDto>> response =
                controller.getIsGuestTokenYn(request(" "));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatusCode()).isEqualTo("4010");
        verifyNoInteractions(userService);
    }

    @Test
    void resolvesGuestUserIdFromUuidInTokenWhenBodyUuidIsMissing() {
        String uuid = "guest-uuid";
        String token = createToken(null, uuid, Instant.now().plusSeconds(600));
        User guestUser = mock(User.class);
        when(userService.validUUID(uuid)).thenReturn(true);
        when(userService.getUserByUUID(uuid)).thenReturn(Optional.of(guestUser));
        when(guestUser.getId()).thenReturn(42L);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        ResponseEntity<CommonApiResponse<com.wemeetnow.auth_service.dto.GuestUserDto>> response =
                controller.getUserIdFromGuestToken(new GuestIdRequestDto(), httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getUserId()).isEqualTo(42L);
        assertThat(response.getBody().getData().getUuId()).isEqualTo(uuid);
        verify(userService).getUserByUUID(uuid);
    }

    @Test
    void rejectsBodyUuidThatDoesNotMatchGuestToken() {
        String token = createToken(null, "signed-uuid", Instant.now().plusSeconds(600));
        GuestIdRequestDto request = new GuestIdRequestDto();
        request.setUuid("different-uuid");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        ResponseEntity<CommonApiResponse<com.wemeetnow.auth_service.dto.GuestUserDto>> response =
                controller.getUserIdFromGuestToken(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(userService);
    }

    private String createToken(Long userId, String uuid, Instant expiration) {
        var builder = Jwts.builder();
        if (userId != null) {
            builder.claim("userId", userId);
        }
        if (uuid != null) {
            builder.claim("uuId", uuid);
        }
        return builder
                .setExpiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private GuestCheckRequestDto request(String token) {
        GuestCheckRequestDto request = new GuestCheckRequestDto();
        request.setGuestToken(token);
        return request;
    }
}
