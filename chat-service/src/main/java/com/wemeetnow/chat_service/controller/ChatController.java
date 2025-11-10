package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.domain.Chat;
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

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatResponseDto sendMessage(@Payload ChatDto chatDto) throws Exception {
        log.info("chatDto: {}", chatDto);
        // DB 저장 (단순 ID 참조 방식)
        return chatService.saveMessage(
                chatDto.getChatRoomId(),
                chatDto.getUserId(),
                chatDto.getMessage(),
                chatDto.getChatType()
        );
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatResponseDto addUser(@Payload ChatDto chatMessageDto) throws Exception {
        // 입장 메시지 강제로 세팅
        String content = chatMessageDto.getUserId() + " 님이 입장했습니다.";
        log.info("chatMessageDto: {}", chatMessageDto);
        // DB 저장
        return chatService.saveMessage(
                chatMessageDto.getChatRoomId(),
                chatMessageDto.getUserId(),
                content,
                ChatType.ENTER // chatType 예: ENTER
        );
    }
    
    // TODO LEAVE 기능 만들기
}