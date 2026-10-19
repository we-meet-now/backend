package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.client.AuthServiceClient;
import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRead;
import com.wemeetnow.chat_service.repository.ChatParticipantRepository;
import com.wemeetnow.chat_service.repository.ChatReadRepository;
import com.wemeetnow.chat_service.repository.ChatRepository;
import com.wemeetnow.chat_service.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceReadMarkTest {

    private static final Long ROOM_ID = 5L;
    private static final Long USER_ID = 11L;
    private static final Long CHAT_ID = 23L;

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ChatReadRepository chatReadRepository;
    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private ChatParticipantRepository chatParticipantRepository;
    @Mock
    private EnterCodeGenerator enterCodeGenerator;
    @Mock
    private AuthServiceClient authServiceClient;

    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() {
        chatRoomService = new ChatRoomService(
                chatRoomRepository,
                chatRepository,
                chatReadRepository,
                restClientBuilder,
                chatParticipantRepository,
                enterCodeGenerator,
                authServiceClient
        );
    }

    @Test
    void marksUnreadChatsRead() throws NoSuchMethodException {
        Chat unreadChat = Chat.builder().chatId(CHAT_ID).chatRoomId(ROOM_ID).userId(9L).build();
        when(chatRepository.findUnreadChatsForUser(eq(ROOM_ID), eq(USER_ID), any()))
                .thenReturn(List.of(unreadChat));
        when(chatReadRepository.existsByChatIdAndUserId(CHAT_ID, USER_ID)).thenReturn(false);

        var response = chatRoomService.enterRoomAndMarkRead(ROOM_ID, USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(ApiStatusCode.SUCCESS.getCode());
        assertThat(response.getMarkedReadCount()).isEqualTo(1);
        verify(chatReadRepository).save(argThat(read -> read.getChatId().equals(CHAT_ID)
                && read.getUserId().equals(USER_ID)));
        assertThat(ChatRoomService.class
                .getMethod("enterRoomAndMarkRead", Long.class, Long.class)
                .getAnnotation(Transactional.class).readOnly()).isFalse();
    }

    @Test
    void propagatesReadMarkingFailureInsteadOfReturningSuccessShapedResponse() {
        Chat unreadChat = Chat.builder().chatId(CHAT_ID).chatRoomId(ROOM_ID).userId(9L).build();
        when(chatRepository.findUnreadChatsForUser(eq(ROOM_ID), eq(USER_ID), any()))
                .thenReturn(List.of(unreadChat));
        when(chatReadRepository.existsByChatIdAndUserId(CHAT_ID, USER_ID)).thenReturn(false);
        when(chatReadRepository.save(any(ChatRead.class))).thenThrow(new IllegalStateException("write failed"));

        assertThatThrownBy(() -> chatRoomService.enterRoomAndMarkRead(ROOM_ID, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("write failed");
    }
}
