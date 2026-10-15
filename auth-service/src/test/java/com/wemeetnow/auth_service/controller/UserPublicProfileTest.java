package com.wemeetnow.auth_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.Friend;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.FriendStatus;
import com.wemeetnow.auth_service.dto.FriendTableRowDto;
import com.wemeetnow.auth_service.dto.UserTableRowDto;
import com.wemeetnow.auth_service.controller.friend.FriendController;
import com.wemeetnow.auth_service.service.FriendService;
import com.wemeetnow.auth_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;

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

    @Test
    void allUsersReturnsAllTableColumnsWithoutRecursiveFriendGraph() {
        UserService userService = mock(UserService.class);
        User user = User.builder()
                .username("table-user")
                .email("table@example.com")
                .password("$2a$hashed")
                .phoneNumber("01012345678")
                .build();
        ReflectionTestUtils.setField(user, "id", 11L);
        when(userService.getAllUsers()).thenReturn(List.of(user));

        UserApiController controller = new UserApiController(userService, mock(JwtUtil.class));
        UserTableRowDto row = controller.getUsersAll().getBody().getData().get(0);

        assertThat(row.getUserId()).isEqualTo(11L);
        assertThat(row.getEmail()).isEqualTo("table@example.com");
        assertThat(row.getPassword()).isEqualTo("$2a$hashed");
        assertThat(row.getPhoneNumber()).isEqualTo("01012345678");
    }

    @Test
    void allFriendsReturnsTableColumnsAndUserForeignKey() {
        UserService userService = mock(UserService.class);
        FriendService friendService = mock(FriendService.class);
        User user = mock(User.class);
        Friend friend = mock(Friend.class);
        when(friend.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(15L);
        when(friend.getId()).thenReturn(21L);
        when(friend.getSenderId()).thenReturn(16L);
        when(friend.getFriendStatus()).thenReturn(FriendStatus.NEW);
        when(friendService.getAllFriends()).thenReturn(List.of(friend));

        FriendController controller = new FriendController(userService, friendService, mock(JwtUtil.class));
        FriendTableRowDto row = controller.getAllFriends().getBody().getData().get(0);

        assertThat(row.getFriendId()).isEqualTo(21L);
        assertThat(row.getUserId()).isEqualTo(15L);
        assertThat(row.getSenderId()).isEqualTo(16L);
        assertThat(row.getFriendStatus()).isEqualTo(FriendStatus.NEW);
    }
}
