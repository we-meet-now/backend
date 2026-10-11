package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
}
