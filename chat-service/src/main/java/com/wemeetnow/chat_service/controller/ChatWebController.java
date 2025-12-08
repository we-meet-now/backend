package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.dto.AuthUserResponse;
import com.wemeetnow.chat_service.service.ChatRoomService;
import com.wemeetnow.chat_service.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatWebController {
    private final ChatService chatService;
    private final ChatRoomService chatRoomService;
    private final String AUTH_HEADER = "Authorization";


    @PostMapping("/check-access-token")
    public void checkAccessToken(@RequestHeader(AUTH_HEADER) String token) {
        try {
            AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
            log.info("authUserResponse.getUserId(): {}", authUserResponse.getUserId());
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
        }
    }
}
