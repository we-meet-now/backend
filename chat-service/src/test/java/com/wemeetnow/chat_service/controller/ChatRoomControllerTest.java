package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.dto.AuthGuestResponse;
import com.wemeetnow.chat_service.dto.AuthUserResponse;
import com.wemeetnow.chat_service.dto.ChatResponseDto;
import com.wemeetnow.chat_service.dto.CreateChatRoomRequestDto;
import com.wemeetnow.chat_service.dto.EnterRoomRequestDto;
import com.wemeetnow.chat_service.dto.EnterRoomResponseDto;
import com.wemeetnow.chat_service.dto.GuestYnDto;
import com.wemeetnow.chat_service.dto.GuestBrowserSessionDto;
import com.wemeetnow.chat_service.dto.InviteChatParticipantsRequestDto;
import com.wemeetnow.chat_service.dto.RemoveChatParticipantsRequestDto;
import com.wemeetnow.chat_service.exception.ChatRoomInviteException;
import com.wemeetnow.chat_service.service.ChatRoomService;
import com.wemeetnow.chat_service.service.ChatGuestTokenInfo;
import com.wemeetnow.chat_service.service.ChatService;
import com.wemeetnow.chat_service.service.CommonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import jakarta.servlet.http.Cookie;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatRoomControllerTest {

    private ChatRoomService chatRoomService;
    private ChatService chatService;
    private CommonService commonService;
    private ChatRoomController controller;

    @BeforeEach
    void setUp() {
        chatRoomService = mock(ChatRoomService.class);
        chatService = mock(ChatService.class);
        commonService = mock(CommonService.class);
        controller = new ChatRoomController(chatRoomService, chatService, commonService);
    }

    @Test
    void returnsChatRoomsForAuthenticatedUser() {
        AuthUserResponse authUser = new AuthUserResponse();
        authUser.setUserId(9L);
        when(chatService.isValidAccessToken("Bearer access-token")).thenReturn(authUser);
        when(chatRoomService.findByUserId(9L)).thenReturn(List.of());

        var response = controller.getChatRoomListByUserId("Bearer access-token", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getLoginedUserId()).isEqualTo(9L);
        assertThat(response.getBody().getData().getChatRoomList()).isEmpty();
        verify(chatService).isValidAccessToken("Bearer access-token");
        verify(chatService, never()).isValidGuestToken(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void returnsChatRoomsForGuestUsingUuidAndGuestToken() {
        AuthGuestResponse guestUser = AuthGuestResponse.builder()
                .userId(12L)
                .uuId("guest-uuid")
                .build();
        when(chatService.isValidGuestToken("Bearer guest-token", "guest-uuid")).thenReturn(guestUser);
        when(chatRoomService.findByUserId(12L)).thenReturn(List.of());

        var response = controller.getChatRoomListByUserId("Bearer guest-token", "guest-uuid");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getLoginedUserId()).isEqualTo(12L);
        assertThat(response.getBody().getData().getChatRoomList()).isEmpty();
        verify(chatService).isValidGuestToken("Bearer guest-token", "guest-uuid");
        verify(chatService, never()).isValidAccessToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsMissingIdentity() {
        assertThatThrownBy(() -> controller.getChatRoomListByUserId(null, null))
                .isInstanceOf(ChatRoomInviteException.class)
                .hasMessage("AccessToken 또는 게스트 브라우저 세션 쿠키가 필요합니다.");

        verify(chatRoomService, never()).findByUserId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void returnsChatRoomsForBrowserGuestUsingHttpOnlySessionCookie() {
        GuestBrowserSessionDto guest = new GuestBrowserSessionDto();
        guest.setUserId(18L);
        guest.setUuid("browser-guest-uuid");
        when(commonService.fetchGuestBrowserSessionFromAuthService("opaque-session-id"))
                .thenReturn(guest);
        when(chatRoomService.findByUserId(18L)).thenReturn(List.of());

        var response = controller.getChatRoomListByUserId(null, null, "opaque-session-id");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getLoginedUserId()).isEqualTo(18L);
        verify(commonService).fetchGuestBrowserSessionFromAuthService("opaque-session-id");
        verify(chatService, never()).isValidAccessToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsExpiredBrowserGuestSessionCookie() {
        when(commonService.fetchGuestBrowserSessionFromAuthService("expired-session-id"))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> controller.getChatRoomListByUserId(null, null, "expired-session-id"))
                .isInstanceOf(ChatRoomInviteException.class)
                .hasMessage("유효하지 않거나 만료된 게스트 브라우저 세션입니다.");
        verify(chatRoomService, never()).findByUserId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void returnsRoomHistoryForBrowserGuestUsingCookie() {
        GuestBrowserSessionDto guest = new GuestBrowserSessionDto();
        guest.setUserId(18L);
        guest.setUuid("server-side-uuid");
        when(commonService.fetchGuestBrowserSessionFromAuthService("opaque-session-id"))
                .thenReturn(guest);
        when(chatService.getChatList(5L, 18L)).thenReturn(List.of());

        var response = controller.enterChatRoom(
                5L,
                new MockHttpServletRequest(),
                null,
                "opaque-session-id"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(response.getBody().getData().getLoginedUserId()).isEqualTo(18L);
        verify(commonService).fetchGuestBrowserSessionFromAuthService("opaque-session-id");
    }

    @Test
    void rejectsGuestUuidWithoutGuestToken() {
        assertThatThrownBy(() -> controller.getChatRoomListByUserId(null, "guest-uuid"))
                .isInstanceOf(ChatRoomInviteException.class)
                .hasMessage("게스트 UUID와 게스트 토큰이 필요합니다.");

        verify(chatService, never()).isValidGuestToken(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(chatRoomService, never()).findByUserId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsInvalidGuestTokenAndUuid() {
        when(chatService.isValidGuestToken("Bearer bad-token", "guest-uuid"))
                .thenThrow(new IllegalArgumentException("invalid guest token"));

        assertThatThrownBy(() -> controller.getChatRoomListByUserId("Bearer bad-token", "guest-uuid"))
                .isInstanceOf(ChatRoomInviteException.class)
                .hasMessage("유효하지 않은 게스트 토큰 또는 UUID입니다.");

        verify(chatRoomService, never()).findByUserId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsInvalidAccessToken() {
        when(chatService.isValidAccessToken(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new IllegalArgumentException("invalid access token"));

        assertThatThrownBy(() -> controller.getChatRoomListByUserId("invalid-token", null))
                .isInstanceOf(ChatRoomInviteException.class)
                .hasMessage("유효하지 않은 AccessToken입니다.");

        verify(chatRoomService, never()).findByUserId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void doesNotConvertAuthServiceServerErrorsIntoUnauthorizedResponses() {
        HttpServerErrorException authServiceError =
                new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(chatService.isValidAccessToken("auth-service-error")).thenThrow(authServiceError);

        assertThatThrownBy(() -> controller.getChatRoomListByUserId("auth-service-error", null))
                .isSameAs(authServiceError);
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
        when(chatService.getChatResponseList(
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(ChatResponseDto.builder()
                        .chatId(23L)
                        .chatRoomId(roomId)
                        .userId(8L)
                        .username("author-name")
                        .message("Hello")
                        .build()));

        var response = controller.enterRoomAndMarkRead(request(roomId), requestWithToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getLoginedUserId()).isEqualTo(userId);
        assertThat(response.getBody().getChatList()).hasSize(1);
        assertThat(response.getBody().getChatList().get(0).getChatId()).isEqualTo(23L);
        assertThat(response.getBody().getChatList().get(0).getUsername()).isEqualTo("author-name");
        verify(chatRoomService).isMember(roomId, userId);
    }

    @Test
    void issuesNewGuestSessionWhenEnteringWithoutAuthorization() {
        ChatGuestTokenInfo guestSession = new ChatGuestTokenInfo();
        guestSession.setUuId("new-guest-uuid");
        guestSession.setGuestToken("short-lived-guest-token");
        guestSession.setRefreshToken("opaque-refresh-secret");
        guestSession.setGuestTokenExpiresInSeconds(1800);
        when(chatRoomService.generateGuestSession()).thenReturn(guestSession);

        var response = controller.enterRoomAndMarkRead(request(5L), new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(response.getBody().getUuId()).isEqualTo("new-guest-uuid");
        assertThat(response.getBody().getGuestToken()).isEqualTo("short-lived-guest-token");
        assertThat(response.getBody().getRefreshToken()).isEqualTo("opaque-refresh-secret");
        assertThat(response.getBody().getGuestTokenExpiresInSeconds()).isEqualTo(1800);
        verify(chatRoomService).generateGuestSession();
    }

    @Test
    void entersRoomForBrowserGuestWithoutReturningUuidOrTokens() {
        GuestBrowserSessionDto guest = new GuestBrowserSessionDto();
        guest.setUserId(18L);
        guest.setUuid("server-side-uuid");
        when(commonService.fetchGuestBrowserSessionFromAuthService("opaque-session-id"))
                .thenReturn(guest);
        when(chatRoomService.isMember(5L, 18L)).thenReturn(true);
        when(chatRoomService.enterRoomAndMarkRead(5L, 18L))
                .thenReturn(new EnterRoomResponseDto("2000", "success", 0));
        when(chatService.getChatList(5L, 18L)).thenReturn(List.of());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("guest_session", "opaque-session-id"));
        var response = controller.enterRoomAndMarkRead(request(5L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(response.getBody().getLoginedUserId()).isEqualTo(18L);
        assertThat(response.getBody().getUuId()).isNull();
        assertThat(response.getBody().getGuestToken()).isNull();
        assertThat(response.getBody().getRefreshToken()).isNull();
        verify(commonService).fetchGuestBrowserSessionFromAuthService("opaque-session-id");
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
