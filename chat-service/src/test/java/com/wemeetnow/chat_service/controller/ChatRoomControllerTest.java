package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.dto.AuthUserResponse;
import com.wemeetnow.chat_service.dto.CreateChatRoomRequestDto;
import com.wemeetnow.chat_service.dto.EnterRoomRequestDto;
import com.wemeetnow.chat_service.dto.EnterRoomResponseDto;
import com.wemeetnow.chat_service.dto.GuestYnDto;
import com.wemeetnow.chat_service.dto.InviteChatParticipantsRequestDto;
import com.wemeetnow.chat_service.dto.RemoveChatParticipantsRequestDto;
import com.wemeetnow.chat_service.service.ChatRoomService;
import com.wemeetnow.chat_service.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatRoomControllerTest {

    private ChatRoomService chatRoomService;
    private ChatService chatService;
    private ChatRoomController controller;

    @BeforeEach
    void setUp() {
        chatRoomService = mock(ChatRoomService.class);
        chatService = mock(ChatService.class);
        controller = new ChatRoomController(chatRoomService, chatService);
    }

    @Test
    void rejectsNonParticipantWithoutMarkingReadOrReturningHistory() {
        Long roomId = 5L;
        Long userId = 6L;
        mockLogin(userId);
        when(chatRoomService.isMember(roomId, userId)).thenReturn(false);

        var response = controller.enterRoomAndMarkRead(request(roomId), requestWithToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getStatusCode()).isEqualTo("4030");
        assertThat(response.getBody().getChatList()).isNull();
        verify(chatRoomService, never()).enterRoomAndMarkRead(roomId, userId);
        verify(chatService, never()).getChatList(roomId, userId);
    }

    @Test
    void returnsChatHistoryForParticipant() {
        Long roomId = 5L;
        Long userId = 6L;
        mockLogin(userId);
        when(chatRoomService.isMember(roomId, userId)).thenReturn(true);
        when(chatRoomService.enterRoomAndMarkRead(roomId, userId))
                .thenReturn(new EnterRoomResponseDto("2000", "success", 0));
        when(chatService.getChatList(roomId, userId)).thenReturn(List.of(Chat.builder()
                .chatId(23L)
                .chatRoomId(roomId)
                .userId(8L)
                .message("Hello")
                .build()));

        var response = controller.enterRoomAndMarkRead(request(roomId), requestWithToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getLoginedUserId()).isEqualTo(userId);
        assertThat(response.getBody().getChatList()).hasSize(1);
        assertThat(response.getBody().getChatList().get(0).getChatId()).isEqualTo(23L);
        verify(chatRoomService).isMember(roomId, userId);
    }

    @Test
    void returnsInvitationResultForAuthorizedCaller() {
        AuthUserResponse authUser = new AuthUserResponse();
        authUser.setUserId(9L);
        when(chatService.isValidAccessToken("Bearer test-token")).thenReturn(authUser);
        when(chatRoomService.inviteParticipants(5L, 9L, List.of(1L, 2L), "Bearer test-token"))
                .thenReturn(List.of(1L, 2L));

        InviteChatParticipantsRequestDto requestDto = new InviteChatParticipantsRequestDto();
        requestDto.setRoomId(5L);
        requestDto.setInvitedUserIdList(List.of(1L, 2L));

        var response = controller.inviteChatParticipants("Bearer test-token", requestDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatusCode()).isEqualTo("2000");
        assertThat(response.getBody().getData().getInvitedUserIdList()).containsExactly(1L, 2L);
        assertThat(response.getBody().getData().getInvitedCount()).isEqualTo(2);
    }

    @Test
    void removesRequestedParticipantsUsingJwtUserAsRequester() {
        String token = "Bearer jwt-token";
        AuthUserResponse authUser = new AuthUserResponse();
        authUser.setUserId(9L);
        when(chatService.isValidAccessToken(token)).thenReturn(authUser);
        when(chatRoomService.removeParticipants(5L, 9L, List.of(1L, 2L), token))
                .thenReturn(List.of(1L, 2L));

        RemoveChatParticipantsRequestDto requestDto = new RemoveChatParticipantsRequestDto();
        requestDto.setRoomId(5L);
        requestDto.setRemovedUserIdList(List.of(1L, 2L));

        var response = controller.removeChatParticipants(token, requestDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatusCode()).isEqualTo("2000");
        assertThat(response.getBody().getData().getRemovedUserIdList()).containsExactly(1L, 2L);
        assertThat(response.getBody().getData().getRemovedCount()).isEqualTo(2);
        verify(chatRoomService).removeParticipants(5L, 9L, List.of(1L, 2L), token);
    }

    @Test
    void createsRoomWithCreatorFromAuthorizationToken() {
        String token = "Bearer jwt-token";
        AuthUserResponse authUser = new AuthUserResponse();
        authUser.setUserId(9L);
        when(chatService.isValidAccessToken(token)).thenReturn(authUser);

        CreateChatRoomRequestDto requestDto = new CreateChatRoomRequestDto();
        requestDto.setChatRoomNm("room");
        ChatRoom chatRoom = ChatRoom.builder().chatRoomNm("room").build();
        chatRoom.setChatRoomId(12L);
        chatRoom.setEnterCode("ABCD");
        when(chatRoomService.createChatRoom("9", "room")).thenReturn(chatRoom);

        var response = controller.createChatRoom(token, requestDto);

        assertThat(response.getBody().getData().getChatRoomId()).isEqualTo(12L);
        verify(chatRoomService).createChatRoom("9", "room");
    }

    @Test
    void createsRoomWithParticipantsAndCreatorFromAuthorizationToken() {
        String token = "Bearer jwt-token";
        AuthUserResponse authUser = new AuthUserResponse();
        authUser.setUserId(9L);
        when(chatService.isValidAccessToken(token)).thenReturn(authUser);

        CreateChatRoomRequestDto requestDto = new CreateChatRoomRequestDto();
        requestDto.setChatRoomNm("room");
        requestDto.setParticipantIds(List.of(9L, 10L));
        ChatRoom chatRoom = ChatRoom.builder().chatRoomNm("room").build();
        chatRoom.setChatRoomId(13L);
        chatRoom.setEnterCode("EFGH");
        when(chatRoomService.createChatRoomWithParticipants("9", "room", List.of(9L, 10L)))
                .thenReturn(chatRoom);

        var response = controller.createChatRoomWithParticipants(token, requestDto);

        assertThat(response.getBody().getData().getChatRoomId()).isEqualTo(13L);
        verify(chatRoomService).createChatRoomWithParticipants("9", "room", List.of(9L, 10L));
    }

    private void mockLogin(Long userId) {
        GuestYnDto guestYnDto = new GuestYnDto();
        guestYnDto.setIsGuestYn("N");
        when(chatRoomService.isGuestToken("test-token")).thenReturn(guestYnDto);

        AuthUserResponse authUserResponse = new AuthUserResponse();
        authUserResponse.setUserId(userId);
        when(chatService.isValidAccessToken("test-token")).thenReturn(authUserResponse);
    }

    private EnterRoomRequestDto request(Long roomId) {
        EnterRoomRequestDto requestDto = new EnterRoomRequestDto();
        requestDto.setRoomId(roomId);
        return requestDto;
    }

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token");
        return request;
    }
}
