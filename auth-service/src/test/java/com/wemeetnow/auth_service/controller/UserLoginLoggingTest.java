package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.UserLog;
import com.wemeetnow.auth_service.dto.UserJoinRequestDto;
import com.wemeetnow.auth_service.dto.UserJoinResponseDto;
import com.wemeetnow.auth_service.dto.UserLoginRequestDto;
import com.wemeetnow.auth_service.dto.UserLoginResponseDto;
import com.wemeetnow.auth_service.service.UserLogPersistenceService;
import com.wemeetnow.auth_service.service.UserLogService;
import com.wemeetnow.auth_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserLoginLoggingTest {

    private UserService userService;
    private UserLogPersistenceService persistenceService;
    private UserApiController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        persistenceService = mock(UserLogPersistenceService.class);
        UserLogService userLogService = new UserLogService(persistenceService);
        controller = new UserApiController(userService, mock(JwtUtil.class), userLogService);
    }

    @Test
    void recordsSuccessfulLogin() {
        UserLoginRequestDto requestDto = new UserLoginRequestDto("person@example.com", "password");
        MockHttpServletRequest request = loginRequest();
        when(userService.login(requestDto)).thenReturn(new UserLoginResponseDto("access", "refresh"));
        when(userService.getUserIdByEmail("person@example.com")).thenReturn(Optional.of(42L));

        var response = controller.login(requestDto, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        ArgumentCaptor<UserLog> captor = ArgumentCaptor.forClass(UserLog.class);
        verify(persistenceService).insert(captor.capture());
        assertThat(captor.getValue().getInpUserId()).isEqualTo("42");
        assertThat(captor.getValue().getResponseStatus()).isEqualTo(202);
    }

    @Test
    void recordsFailedLoginBeforeRethrowingAuthenticationError() {
        UserLoginRequestDto requestDto = new UserLoginRequestDto("person@example.com", "wrong-password");
        MockHttpServletRequest request = loginRequest();
        when(userService.login(requestDto)).thenThrow(new IllegalArgumentException("invalid credentials"));
        when(userService.getUserIdByEmail("person@example.com")).thenReturn(Optional.of(42L));

        assertThatThrownBy(() -> controller.login(requestDto, request))
                .isInstanceOf(IllegalArgumentException.class);

        ArgumentCaptor<UserLog> captor = ArgumentCaptor.forClass(UserLog.class);
        verify(persistenceService).insert(captor.capture());
        assertThat(captor.getValue().getInpUserId()).isEqualTo("42");
        assertThat(captor.getValue().getResponseStatus()).isEqualTo(500);
    }

    @Test
    void leavesUserIdNullWhenFailedLoginEmailHasNoPersistedUser() {
        UserLoginRequestDto requestDto = new UserLoginRequestDto("not-found@example.com", "wrong-password");
        MockHttpServletRequest request = loginRequest();
        when(userService.login(requestDto)).thenThrow(new IllegalArgumentException("invalid credentials"));
        when(userService.getUserIdByEmail("not-found@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.login(requestDto, request))
                .isInstanceOf(IllegalArgumentException.class);

        ArgumentCaptor<UserLog> captor = ArgumentCaptor.forClass(UserLog.class);
        verify(persistenceService).insert(captor.capture());
        assertThat(captor.getValue().getInpUserId()).isNull();
        assertThat(captor.getValue().getResponseStatus()).isEqualTo(500);
    }

    @Test
    void recordsSuccessfulJoin() {
        UserJoinRequestDto requestDto = UserJoinRequestDto.builder()
                .email("new-user@example.com")
                .password("password")
                .passwordCorrect("password")
                .username("new-user")
                .build();
        when(userService.join(requestDto)).thenReturn(new UserJoinResponseDto(4L, requestDto.getEmail()));
        when(userService.getUserIdByEmail("new-user@example.com")).thenReturn(Optional.of(4L));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/v1/users/join");
        request.setRemoteAddr("192.0.2.11");

        var response = controller.join(requestDto, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ArgumentCaptor<UserLog> captor = ArgumentCaptor.forClass(UserLog.class);
        verify(persistenceService).insert(captor.capture());
        assertThat(captor.getValue().getInpUserId()).isEqualTo("4");
        assertThat(captor.getValue().getMenuLink()).isEqualTo("/api/auth/v1/users/join");
        assertThat(captor.getValue().getResponseStatus()).isEqualTo(201);
    }

    @Test
    void recordsFailedJoinBeforeRethrowingServiceError() {
        UserJoinRequestDto requestDto = UserJoinRequestDto.builder()
                .email("existing@example.com")
                .password("password")
                .passwordCorrect("password")
                .username("existing")
                .build();
        when(userService.join(requestDto)).thenThrow(new IllegalStateException("join failed"));
        when(userService.getUserIdByEmail("existing@example.com")).thenReturn(Optional.of(7L));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/v1/users/join");
        request.setRemoteAddr("192.0.2.12");

        assertThatThrownBy(() -> controller.join(requestDto, request))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<UserLog> captor = ArgumentCaptor.forClass(UserLog.class);
        verify(persistenceService).insert(captor.capture());
        assertThat(captor.getValue().getInpUserId()).isEqualTo("7");
        assertThat(captor.getValue().getResponseStatus()).isEqualTo(500);
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/v1/users/login");
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        return request;
    }
}
