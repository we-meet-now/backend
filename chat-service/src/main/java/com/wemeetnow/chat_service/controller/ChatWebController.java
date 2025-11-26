package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.service.ChatRoomService;
import com.wemeetnow.chat_service.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
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

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/roomId={roomId}")
    public ResponseEntity enterChatRoom(@PathVariable("roomId") Long roomId, HttpServletRequest request) {
        log.info("request: {}", request);
        log.info("roomId: {}", roomId);
        log.info("roomId.getClass: {}", roomId.getClass());
        String statusCode = "5000";
        HttpStatus httpStatus = HttpStatus.OK;
        Long loginedUserId = 0L;
        Map<String, Object> bodyMap = new HashMap<>();
        List<Chat> chatList = new ArrayList<>();
        try {
//            String accessToken = jwtUtil.getAccessTokenFromHeader(request);
//            if (!JwtUtil.isExpired(accessToken)) {
//                loginedUserId = JwtUtil.getId(accessToken);
//                statusCode = "2000";
//                chatRoomList = chatRoomService.findByUserId(loginedUserId);
//            }
            loginedUserId = 1L;
            statusCode = "2000";
            chatList = chatService.getChatList(roomId);
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            statusCode = "5005";
        } finally {
            bodyMap.put("loginedUserId", loginedUserId);
            bodyMap.put("statusCode", statusCode);
            bodyMap.put("chatList", chatList);
        }
        return ResponseEntity.status(httpStatus).body(bodyMap);
    }
}
