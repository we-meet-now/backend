package com.wemeetnow.chat_service.config;

import com.wemeetnow.chat_service.dto.AuthGuestResponse;
import com.wemeetnow.chat_service.dto.AuthUserResponse;
import com.wemeetnow.chat_service.dto.GuestYnDto;
import com.wemeetnow.chat_service.service.ChatRoomService;
import com.wemeetnow.chat_service.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatStompAuthChannelInterceptorTest {

    private ChatRoomService chatRoomService;
    private ChatService chatService;
    private ChatStompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        chatRoomService = mock(ChatRoomService.class);
        chatService = mock(ChatService.class);
        interceptor = new ChatStompAuthChannelInterceptor(chatRoomService, chatService);
    }

    @Test
    void resolvesGuestTokenAndSetsStompPrincipal() {
        GuestYnDto guestYn = new GuestYnDto();
        guestYn.setIsGuestYn("Y");
        when(chatRoomService.isGuestToken("guest-token")).thenReturn(guestYn);
        when(chatService.isValidGuestToken("guest-token"))
                .thenReturn(AuthGuestResponse.builder().userId(42L).uuId("guest-uuid").build());

        Message<?> authenticated = interceptConnect("Bearer guest-token");

        var principal = (ChatUserPrincipal) StompHeaderAccessor.wrap(authenticated).getUser();
        assertThat(principal.getName()).isEqualTo("42");
        assertThat(principal.uuid()).isEqualTo("guest-uuid");
        assertThat(principal.guest()).isTrue();
        verify(chatService).isValidGuestToken("guest-token");
    }

    @Test
    void resolvesAccessTokenAndSetsStompPrincipal() {
        GuestYnDto guestYn = new GuestYnDto();
        guestYn.setIsGuestYn("N");
        AuthUserResponse authUser = new AuthUserResponse();
        authUser.setUserId(17L);
        when(chatRoomService.isGuestToken("access-token")).thenReturn(guestYn);
        when(chatService.isValidAccessToken("access-token")).thenReturn(authUser);

        Message<?> authenticated = interceptConnect("Bearer access-token");

        assertThat(StompHeaderAccessor.wrap(authenticated).getUser().getName()).isEqualTo("17");
        verify(chatService).isValidAccessToken("access-token");
    }

    @Test
    void resolvesAuthorizationHeaderOnSendWhenNotProvidedAtConnect() {
        GuestYnDto guestYn = new GuestYnDto();
        guestYn.setIsGuestYn("Y");
        when(chatRoomService.isGuestToken("guest-token")).thenReturn(guestYn);
        when(chatService.isValidGuestToken("guest-token"))
                .thenReturn(AuthGuestResponse.builder().userId(42L).uuId("guest-uuid").build());

        Message<?> authenticated = intercept(StompCommand.SEND, "Bearer guest-token");

        assertThat(StompHeaderAccessor.wrap(authenticated).getUser().getName()).isEqualTo("42");
    }

    private Message<?> interceptConnect(String authorizationHeader) {
        return intercept(StompCommand.CONNECT, authorizationHeader);
    }

    private Message<?> intercept(StompCommand command, String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.addNativeHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return interceptor.preSend(message, mock(MessageChannel.class));
    }
}
