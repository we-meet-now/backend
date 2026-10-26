package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.config.ChatUserPrincipal;
import com.wemeetnow.chat_service.domain.enums.ChatType;
import com.wemeetnow.chat_service.dto.ChatDto;
import com.wemeetnow.chat_service.dto.ChatResponseDto;
import com.wemeetnow.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    //    @MessageMapping("/chat.sendMessage")
//    @SendTo("/topic/public")
//    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
//        System.out.println("chatMessage.getContent() = " + chatMessage.getContent());
//        return chatMessage;
//    }
//
//    @MessageMapping("/chat.addUser")
//    @SendTo("/topic/public")
//    public ChatMessage addUser(@Payload ChatMessage chatMessage) {
//        System.out.println("chatMessage.getContent() = " + chatMessage.getContent());
//        chatMessage.setContent(chatMessage.getSender() + " 님이 입장했습니다.");
//        return chatMessage;
//    }
    @CrossOrigin(origins = "http://localhost:5173")
    @MessageMapping("/chat.sendMessage")
    @SendTo("/api/chat/v1/message/topic/public")
    public ChatResponseDto sendMessage(@Payload ChatDto chatDto, Principal principal) throws Exception {
        Long userId = authenticatedUserId(chatDto, principal);
        log.info("chat message: roomId={}, userId={}, chatType={}",
                chatDto.getChatRoomId(), userId, chatDto.getChatType());
        return chatService.saveMessage(
                chatDto.getChatRoomId(),
                userId,
                chatDto.getMessage(),
                chatDto.getChatType(),
                ((ChatUserPrincipal) principal).username()
        );
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @MessageMapping("/chat.addUser")
    @SendTo("/api/chat/v1/message/topic/public")
    public ChatResponseDto addUser(@Payload ChatDto chatMessageDto, Principal principal) throws Exception {
        Long userId = authenticatedUserId(chatMessageDto, principal);
        String content = userId + " 님이 입장했습니다.";
        log.info("chat participant joined: roomId={}, userId={}", chatMessageDto.getChatRoomId(), userId);
        return chatService.saveMessage(
                chatMessageDto.getChatRoomId(),
                userId,
                content,
                ChatType.ENTER,
                ((ChatUserPrincipal) principal).username()
        );
    }

    private Long authenticatedUserId(ChatDto chatDto, Principal principal) {
        if (!(principal instanceof ChatUserPrincipal authenticatedUser)) {
            throw new IllegalArgumentException("WebSocket 연결에 인증된 토큰이 없습니다.");
        }

        boolean hasGuestUuid = chatDto.getUuId() != null && !chatDto.getUuId().isBlank();
        boolean hasUserId = chatDto.getUserId() != null;
        if (hasGuestUuid == hasUserId) {
            throw new IllegalArgumentException("게스트는 uuId, 로그인 사용자는 userId 중 하나만 전달해야 합니다.");
        }

        if (authenticatedUser.guest()) {
            if (!hasGuestUuid || !authenticatedUser.uuid().equals(chatDto.getUuId())) {
                throw new IllegalArgumentException("요청 uuId가 인증된 게스트와 일치하지 않습니다.");
            }
        } else if (!hasUserId || !authenticatedUser.userId().equals(chatDto.getUserId())) {
            throw new IllegalArgumentException("요청 userId가 인증된 사용자와 일치하지 않습니다.");
        }
        return authenticatedUser.userId();
    }
}