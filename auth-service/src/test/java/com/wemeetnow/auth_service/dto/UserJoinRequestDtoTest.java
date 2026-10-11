package com.wemeetnow.auth_service.dto;

import com.wemeetnow.auth_service.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserJoinRequestDtoTest {

    @Test
    void toEntityCopiesUsername() {
        UserJoinRequestDto request = UserJoinRequestDto.builder()
                .username("invited-user")
                .email("invited-user@example.com")
                .nickname("Invitee")
                .build();

        User user = request.toEntity("encoded-password", "LOCAL");

        assertThat(user.getUsername()).isEqualTo("invited-user");
    }
}
