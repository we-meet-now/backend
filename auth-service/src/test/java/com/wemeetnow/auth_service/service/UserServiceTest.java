package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.ErrorCode;
import com.wemeetnow.auth_service.dto.SetUsernameResponseDto;
import com.wemeetnow.auth_service.exception.CustomException;
import com.wemeetnow.auth_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void returnsEmptyWhenUserIdDoesNotExist() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findById(100L)).thenReturn(Optional.empty());
        UserService userService = new UserService(
                mock(BCryptPasswordEncoder.class),
                userRepository,
                mock(JwtUtil.class)
        );

        assertThat(userService.getUserById(100L)).isEmpty();
    }

    @Test
    void updatesUsernameWhenItIsAvailable() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserService(
                mock(BCryptPasswordEncoder.class),
                userRepository,
                mock(JwtUtil.class)
        );
        User user = User.builder().username("old-name").build();
        when(userRepository.existsByUsername("new-name")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        SetUsernameResponseDto response = userService.updateUsername(user, "new-name");

        assertThat(user.getUsername()).isEqualTo("new-name");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getUsername()).isEqualTo("new-name");
        verify(userRepository).save(user);
    }

    @Test
    void rejectsUsernameThatBelongsToAnotherUser() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserService(
                mock(BCryptPasswordEncoder.class),
                userRepository,
                mock(JwtUtil.class)
        );
        User user = User.builder().username("old-name").build();
        when(userRepository.existsByUsername("taken-name")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUsername(user, "taken-name"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);

        verify(userRepository).existsByUsername("taken-name");
        verify(userRepository, never()).save(user);
    }
}
