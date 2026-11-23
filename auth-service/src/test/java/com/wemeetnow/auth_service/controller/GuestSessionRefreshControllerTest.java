package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.UserLog;
import com.wemeetnow.auth_service.dto.GuestSessionRefreshRequestDto;
import com.wemeetnow.auth_service.dto.GuestSessionResponseDto;
import com.wemeetnow.auth_service.service.UserLogPersistenceService;
import com.wemeetnow.auth_service.service.UserLogService;
import com.wemeetnow.auth_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuestSessionRefreshControllerTest {
    private UserService userService;
    private UserLogPersistenceService persistenceService;
    private UserApiController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        persistenceService = mock(UserLogPersistenceService.class);
        controller = new UserApiController(
                userService,
                mock(JwtUtil.class),
                new UserLogService(persistenceService)
        );
    }

    @Test
    void acceptsRefreshTokenBodyAndReturnsRotatedCredentialsWithoutCaching() {
        String uuid = "guest-uuid";
        String previousRefreshToken = "previous-refresh-token";
        GuestSessionRefreshRequestDto requestDto = new GuestSessionRefreshRequestDto();
        requestDto.setRefreshToken(previousRefreshToken);
        GuestSessionResponseDto rotatedCredentials = GuestSessionResponseDto.builder()
                .uuId(uuid)
                .guestToken("rotated-guest-token")
                .refreshToken("rotated-refresh-token")
                .build();
        User guestUser = new User(uuid);
        guestUser.setId(42L);
        when(userService.refreshGuestSession(previousRefreshToken)).thenReturn(rotatedCredentials);
        when(userService.getUserByUUID(uuid)).thenReturn(Optional.of(guestUser));

        var response = controller.refreshGuestSession(
                requestDto,
                new MockHttpServletRequest(
                        "POST",
                        "/api/auth/v1/users/no-login/guest-sessions/refresh"
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(response.getBody().getData().getGuestToken()).isEqualTo("rotated-guest-token");
        assertThat(response.getBody().getData().getRefreshToken()).isEqualTo("rotated-refresh-token");
        verify(userService).refreshGuestSession(previousRefreshToken);

        ArgumentCaptor<UserLog> logCaptor = ArgumentCaptor.forClass(UserLog.class);
        verify(persistenceService).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getInpUserId()).isEqualTo("42");
        assertThat(logCaptor.getValue().getResponseStatus()).isEqualTo(200);
        assertThat(logCaptor.getValue().getMenuLink())
                .isEqualTo("/api/auth/v1/users/no-login/guest-sessions/refresh");
    }
}
