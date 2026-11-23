package com.wemeetnow.chat_service.config;

import com.wemeetnow.chat_service.dto.AuthGuestResponse;
import com.wemeetnow.chat_service.dto.AuthUserResponse;
import com.wemeetnow.chat_service.dto.GuestBrowserSessionDto;
import com.wemeetnow.chat_service.dto.GuestYnDto;
import com.wemeetnow.chat_service.service.ChatRoomService;
import com.wemeetnow.chat_service.service.ChatService;
import com.wemeetnow.chat_service.service.CommonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatStompAuthChannelInterceptorTest {

    private ChatRoomService chatRoomService;
    private ChatService chatService;
    private CommonService commonService;
    private ChatStompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        chatRoomService = mock(ChatRoomService.class);
        chatService = mock(ChatService.class);
        commonService = mock(CommonService.class);
        interceptor = new ChatStompAuthChannelInterceptor(chatRoomService, chatService, commonService);
    }

    @Test
    void resolvesGuestTokenAndSetsStompPrincipal() {
        GuestYnDto guestYn = new GuestYnDto();
        guestYn.setIsGuestYn("Y");
        when(chatRoomService.isGuestToken("guest-token")).thenReturn(guestYn);
        when(chatService.isValidGuestToken("guest-token"))
                .thenReturn(AuthGuestResponse.builder().userId(42L).uuId("guest-uuid")
                        .username("guest-name").guestToken("refreshed-guest-token").isRefreshed("Y").build());

        Message<?> authenticated = interceptConnect("Bearer guest-token");

        var principal = (ChatUserPrincipal) StompHeaderAccessor.wrap(authenticated).getUser();
        assertThat(principal.getName()).isEqualTo("42");
        assertThat(principal.uuid()).isEqualTo("guest-uuid");
        assertThat(principal.guest()).isTrue();
        assertThat(principal.guestToken()).isEqualTo("refreshed-guest-token");
        assertThat(principal.isRefreshed()).isEqualTo("Y");
        assertThat(principal.username()).isEqualTo("guest-name");
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
        when(chatService.getUsername(
                org.mockito.ArgumentMatchers.eq(17L), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("member-name");

        Message<?> authenticated = interceptConnect("Bearer access-token");

        var principal = (ChatUserPrincipal) StompHeaderAccessor.wrap(authenticated).getUser();
        assertThat(principal.getName()).isEqualTo("17");
        assertThat(principal.username()).isEqualTo("member-name");
        verify(chatService).isValidAccessToken("access-token");
        verify(chatService).getUsername(
                org.mockito.ArgumentMatchers.eq(17L), org.mockito.ArgumentMatchers.anyString());
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

    @Test
    void resolvesBrowserGuestFromHandshakeCookieOnConnect() {
        GuestBrowserSessionDto guest = new GuestBrowserSessionDto();
        guest.setUserId(54L);
        guest.setUuid("browser-guest-uuid");
        guest.setUsername("browser-guest");
        when(commonService.fetchGuestBrowserSessionFromAuthService("opaque-session-id"))
                .thenReturn(guest);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(Map.of(
                GuestBrowserSessionHandshakeInterceptor.SESSION_ID_ATTRIBUTE,
                "opaque-session-id"
        ));
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> authenticated = interceptor.preSend(message, mock(MessageChannel.class));

        ChatUserPrincipal principal = (ChatUserPrincipal) StompHeaderAccessor.wrap(authenticated).getUser();
        assertThat(principal.userId()).isEqualTo(54L);
        assertThat(principal.uuid()).isEqualTo("browser-guest-uuid");
        assertThat(principal.guest()).isTrue();
        assertThat(principal.browserSession()).isTrue();
        assertThat(principal.guestToken()).isNull();
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
