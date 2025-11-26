package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.config.jwt.JwtUtil;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.dto.AuthUserDto;
import com.wemeetnow.chat_service.dto.CreateChatRoomRequestDto;
import com.wemeetnow.chat_service.service.ChatRoomService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

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

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("")
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

    @CrossOrigin(origins = "http://localhost:5173")
    @PostMapping("/create-one")
    public ResponseEntity createOnChatRoom(@RequestBody CreateChatRoomRequestDto createChatRoomRequestDto, HttpServletRequest request) {
        HttpStatus httpStatus = HttpStatus.OK;
        String statusCode = "5000";
        Map<String, Object> bodyMap = new HashMap<>();
        List<ChatRoom> chatRoomList = new ArrayList<>();

        try {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            String accessToken = authorizationHeader.replace("Bearer ", "");
            AuthUserDto authUserDto = chatRoomService.fetchUserFromAuthService(accessToken);
//            String accessToken = jwtUtil.getAccessTokenFromHeader(request);
//            if (!JwtUtil.isExpired(accessToken)) {
//                loginedUserId = JwtUtil.getId(accessToken);
//                statusCode = "2000";
//                chatRoomList = chatRoomService.findByUserId(loginedUserId);
//            }

            log.info("authUserDto.getUserId: {}", authUserDto.getUserId());
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            statusCode = "5005";
        } finally {
            bodyMap.put("statusCode", statusCode);
            bodyMap.put("chatRoomList", chatRoomList);
        }
        return ResponseEntity.status(httpStatus).body(bodyMap);
    }
}
