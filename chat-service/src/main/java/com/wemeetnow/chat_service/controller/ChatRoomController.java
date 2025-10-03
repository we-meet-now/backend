package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.config.jwt.JwtUtil;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.service.ChatRoomService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat-rooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;
    private final JwtUtil jwtUtil;

    @GetMapping("/")
    public ResponseEntity getChatRoomListByUserId(HttpServletRequest request, HttpServletResponse response) {
        log.info("request: {}", request);
        HttpStatus httpStatus = HttpStatus.OK;
        Long loginedUserId = 0L;
        String statusCode = "5000";
        Map<String, Object> bodyMap = new HashMap<>();
        List<ChatRoom> chatRoomList = new ArrayList<>();
        try {
//            String accessToken = jwtUtil.getAccessTokenFromHeader(request);
//            if (!JwtUtil.isExpired(accessToken)) {
//                loginedUserId = JwtUtil.getId(accessToken);
//                statusCode = "2000";
//                chatRoomList = chatRoomService.findByUserId(loginedUserId);
//            }
            loginedUserId = 1L;
            statusCode = "2000";
            chatRoomList = chatRoomService.findByUserId(loginedUserId);

        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            statusCode = "5005";
        } finally {
            bodyMap.put("statusCode", statusCode);
            bodyMap.put("loginedUserId", loginedUserId);
            bodyMap.put("chatRoomList", chatRoomList);
        }
        return ResponseEntity.status(httpStatus).body(bodyMap);
    }
}
