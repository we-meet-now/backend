package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.config.ChatUserPrincipal;
import com.wemeetnow.chat_service.domain.enums.ChatType;
import com.wemeetnow.chat_service.dto.ChatDto;
import com.wemeetnow.chat_service.dto.ChatResponseDto;
import com.wemeetnow.chat_service.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatControllerTest {

    private ChatService chatService;
    private ChatController chatController;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        chatController = new ChatController(chatService);
    }

    @Test
    void sendsLoginMessageUsingAuthenticatedPrincipal() throws Exception {
        ChatDto chatDto = new ChatDto();
        chatDto.setChatRoomId(13L);
        chatDto.setUserId(42L);
        chatDto.setMessage("guest message");
        chatDto.setChatType(ChatType.CHAT);
        ChatResponseDto expected = mock(ChatResponseDto.class);
        when(chatService.saveMessage(13L, 42L, "guest message", ChatType.CHAT, "member-name"))
                .thenReturn(expected);

        ChatResponseDto response = chatController.sendMessage(chatDto, principal(42L, null, false, "member-name"));

        assertThat(response).isSameAs(expected);
        verify(chatService).saveMessage(13L, 42L, "guest message", ChatType.CHAT, "member-name");
    }

    @Test
    void sendsGuestMessageUsingTokenIdentityAndMatchingUuid() throws Exception {
        ChatDto chatDto = new ChatDto();
        chatDto.setChatRoomId(13L);
        chatDto.setUuId("guest-uuid");
        chatDto.setMessage("guest message");
        chatDto.setChatType(ChatType.CHAT);
        ChatResponseDto expected = mock(ChatResponseDto.class);
        when(chatService.saveMessage(13L, 42L, "guest message", ChatType.CHAT, null)).thenReturn(expected);

        ChatResponseDto response = chatController.sendMessage(
                chatDto, principal(42L, "guest-uuid", true, null));

        assertThat(response).isSameAs(expected);
        verify(chatService).saveMessage(13L, 42L, "guest message", ChatType.CHAT, null);
    }

    @Test
    void rejectsLoginUserIdThatDoesNotMatchAuthenticatedToken() {
        ChatDto chatDto = new ChatDto();
        chatDto.setChatRoomId(13L);
        chatDto.setUserId(99L);
        chatDto.setMessage("message");
        chatDto.setChatType(ChatType.CHAT);

        assertThatThrownBy(() -> chatController.sendMessage(
                chatDto, principal(42L, null, false, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("요청 userId가 인증된 사용자와 일치하지 않습니다.");
    }

    @Test
    void rejectsGuestUuidThatDoesNotMatchAuthenticatedToken() {
        ChatDto chatDto = new ChatDto();
        chatDto.setChatRoomId(13L);
        chatDto.setUuId("different-uuid");
        chatDto.setMessage("message");
        chatDto.setChatType(ChatType.CHAT);

        assertThatThrownBy(() -> chatController.sendMessage(
                chatDto, principal(42L, "guest-uuid", true, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("요청 uuId가 인증된 게스트와 일치하지 않습니다.");
    }

    @Test
    void rejectsMessagesWithoutAnAuthenticatedPrincipal() {
        ChatDto chatDto = new ChatDto();
        chatDto.setChatRoomId(13L);
        chatDto.setUserId(42L);
        chatDto.setMessage("guest message");
        chatDto.setChatType(ChatType.CHAT);

        assertThatThrownBy(() -> chatController.sendMessage(chatDto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WebSocket 연결에 인증된 토큰이 없습니다.");
    }

    private Principal principal(Long userId, String uuid, boolean guest, String username) {
        return new ChatUserPrincipal(userId, uuid, guest, username);
    }
}
