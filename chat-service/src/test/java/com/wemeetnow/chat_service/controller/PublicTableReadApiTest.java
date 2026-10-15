package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.client.AuthServiceClient;
import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatParticipant;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.service.ChatParticipantService;
import com.wemeetnow.chat_service.service.ChatRoomService;
import com.wemeetnow.chat_service.service.ChatService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicTableReadApiTest {

    @Test
    void returnsAllChatRows() {
        ChatService chatService = mock(ChatService.class);
        Chat chat = Chat.builder().chatId(1L).message("message").build();
        when(chatService.getAllChats()).thenReturn(List.of(chat));

        var response = new ChatWebController(chatService, mock(ChatRoomService.class)).getAllChats();

        assertThat(response.getBody().getStatusCode()).isEqualTo("2000");
        assertThat(response.getBody().getData()).containsExactly(chat);
    }

    @Test
    void returnsAllChatRoomRows() {
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setChatRoomId(2L);
        chatRoom.setEnterCode("entry");
        when(chatRoomService.getAllChatRooms()).thenReturn(List.of(chatRoom));

        var response = new ChatRoomController(chatRoomService, mock(ChatService.class)).getAllChatRooms();

        assertThat(response.getBody().getStatusCode()).isEqualTo("2000");
        assertThat(response.getBody().getData()).containsExactly(chatRoom);
    }

    @Test
    void returnsAllChatParticipantRows() {
        ChatParticipantService participantService = mock(ChatParticipantService.class);
        ChatParticipant participant = ChatParticipant.builder()
                .chatParticipantId(3L)
                .userId(4L)
                .chatRoomId(5L)
                .useYn('Y')
                .build();
        when(participantService.getAllChatParticipants()).thenReturn(List.of(participant));

        var response = new ChatParticipantController(
                mock(ChatRoomService.class),
                participantService,
                mock(AuthServiceClient.class)).getAllChatParticipants();

        assertThat(response.getBody().getStatusCode()).isEqualTo("2000");
        assertThat(response.getBody().getData()).containsExactly(participant);
    }
}
