package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.config.ChatUserPrincipal;
import com.wemeetnow.chat_service.domain.enums.ChatType;
import com.wemeetnow.chat_service.dto.ChatDto;
import com.wemeetnow.chat_service.dto.ChatResponseDto;
import com.wemeetnow.chat_service.dto.GuestTokenRefreshDto;
import com.wemeetnow.chat_service.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.security.Principal;

@Slf4j
@Controller
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    public ChatController(ChatService chatService) {
        this(chatService, null);
    }

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
        Long userId = authenticatedUserId(principal);
        sendRefreshedGuestToken((ChatUserPrincipal) principal);
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
        Long userId = authenticatedUserId(principal);
        sendRefreshedGuestToken((ChatUserPrincipal) principal);
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

    private Long authenticatedUserId(Principal principal) {
        if (!(principal instanceof ChatUserPrincipal authenticatedUser)) {
            throw new IllegalArgumentException("WebSocket 연결에 인증된 토큰이 없습니다.");
        }
        if (authenticatedUser.userId() == null || authenticatedUser.userId() <= 0) {
            throw new IllegalArgumentException("인증된 사용자 정보가 올바르지 않습니다.");
        }
        return authenticatedUser.userId();
    }

    private void sendRefreshedGuestToken(ChatUserPrincipal principal) {
        if (principal.guest() && "Y".equals(principal.isRefreshed())
                && principal.guestToken() != null && !principal.guestToken().isBlank()) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(), "/queue/guest-token",
                    new GuestTokenRefreshDto(principal.guestToken(), principal.isRefreshed()));
        }
    }
}
