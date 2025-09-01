package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.dto.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        System.out.println("chatMessage.getContent() = " + chatMessage.getContent());
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage) {
        System.out.println("chatMessage.getContent() = " + chatMessage.getContent());
        chatMessage.setContent(chatMessage.getSender() + " 님이 입장했습니다.");
        return chatMessage;
    }

}