package com.wemeetnow.chat_service.config;

import com.wemeetnow.chat_service.dto.AuthGuestResponse;
import com.wemeetnow.chat_service.dto.AuthUserResponse;
import com.wemeetnow.chat_service.dto.GuestYnDto;
import com.wemeetnow.chat_service.service.ChatRoomService;
import com.wemeetnow.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class ChatStompAuthChannelInterceptor implements ChannelInterceptor {

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null
                || (accessor.getCommand() != StompCommand.CONNECT && accessor.getCommand() != StompCommand.SEND)) {
            return message;
        }

        String authorizationHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null) {
            return message;
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new MessageDeliveryException(message, "Authorization 헤더는 Bearer 토큰이어야 합니다.");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new MessageDeliveryException(message, "Authorization 토큰이 비어 있습니다.");
        }

        accessor.setUser(resolveUserPrincipal(message, token));
        return message;
    }

    private Principal resolveUserPrincipal(Message<?> message, String token) {
        GuestYnDto guestYn = chatRoomService.isGuestToken(token);
        Long userId;
        String uuid = null;
        boolean isGuest = false;
        if (guestYn != null && "Y".equals(guestYn.getIsGuestYn())) {
            AuthGuestResponse guestUser = chatService.isValidGuestToken(token);
            userId = guestUser.getUserId();
            uuid = guestUser.getUuId();
            isGuest = true;
        } else {
            AuthUserResponse authUser = chatService.isValidAccessToken(token);
            userId = authUser.getUserId();
        }

        if (userId == null || userId <= 0) {
            throw new MessageDeliveryException(message, "토큰에 유효한 사용자 ID가 없습니다.");
        }
        if (isGuest && (uuid == null || uuid.isBlank())) {
            throw new MessageDeliveryException(message, "게스트 토큰에 UUID가 없습니다.");
        }
        return new ChatUserPrincipal(userId, uuid, isGuest);
    }
}
