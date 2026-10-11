package com.wemeetnow.auth_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserPublicProfileTest {

    @Test
    void userLookupReturnsOnlyPublicChatProfileFields() throws Exception {
        UserService userService = mock(UserService.class);
        User user = User.builder()
                .username("chat-user")
                .email("private@example.com")
                .phoneNumber("01012345678")
                .build();
        ReflectionTestUtils.setField(user, "id", 7L);
        when(userService.getUserById(7L)).thenReturn(Optional.of(user));

        UserApiController controller = new UserApiController(userService, mock(JwtUtil.class));
        JsonNode response = new ObjectMapper().valueToTree(controller.getUserById(7L).getBody());

        assertThat(response.at("/data/userId").asLong()).isEqualTo(7L);
        assertThat(response.at("/data/username").asText()).isEqualTo("chat-user");
        assertThat(response.at("/data/email").isMissingNode()).isTrue();
        assertThat(response.at("/data/phoneNumber").isMissingNode()).isTrue();
    }
}
