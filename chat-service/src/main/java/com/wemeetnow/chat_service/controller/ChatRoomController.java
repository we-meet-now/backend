package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.config.jwt.JwtUtil;
import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.dto.*;
import com.wemeetnow.chat_service.service.ChatRoomService;
import com.wemeetnow.chat_service.service.ChatService;
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
    private final ChatService chatService;
    private final String AUTH_HEADER = "Authorization";

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("")
    public ResponseEntity getChatRoomListByUserId(@RequestHeader(AUTH_HEADER) String token, HttpServletRequest request, HttpServletResponse response) {
        log.info("request: {}", request);
        HttpStatus httpStatus = HttpStatus.OK;
        Long loginedUserId = 0L;
        String statusCode = "5000";
        Map<String, Object> bodyMap = new HashMap<>();
        List<ChatRoomWithNotReadCountDto> chatRoomListWithNotReadCount = new ArrayList<>();
        try {
            AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
            loginedUserId = authUserResponse.getUserId();
            statusCode = "2000";
            List<ChatRoom> chatRoomList = chatRoomService.findByUserId(loginedUserId);

            for (ChatRoom chatRoom : chatRoomList) {
                int notReadCount = chatService.getNotReadCountByRoomIdAndUserId(chatRoom.getChatRoomId(), loginedUserId);
                chatRoomListWithNotReadCount.add(new ChatRoomWithNotReadCountDto(chatRoom, notReadCount));
            }
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            statusCode = "5005";
        } finally {
            bodyMap.put("statusCode", statusCode);
            bodyMap.put("loginedUserId", loginedUserId);
            bodyMap.put("chatRoomList", chatRoomListWithNotReadCount);
        }
        return ResponseEntity.status(httpStatus).body(bodyMap);
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/roomId={roomId}")
    public ResponseEntity enterChatRoom(@PathVariable("roomId") Long roomId, HttpServletRequest request, @RequestHeader(AUTH_HEADER) String token) {
        log.info("request: {}", request);
        log.info("roomId: {}", roomId);
        log.info("roomId.getClass: {}", roomId.getClass());
        String statusCode = "5000";
        HttpStatus httpStatus = HttpStatus.OK;
        Long loginedUserId = 0L;
        Map<String, Object> bodyMap = new HashMap<>();
        List<Chat> chatList = new ArrayList<>();
        try {
            AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
            loginedUserId = authUserResponse.getUserId();
            statusCode = "2000";
            chatList = chatService.getChatList(roomId, loginedUserId);
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
    // Add to ChatRoomController.java

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/enter-room/roomId={roomId}")
    public ResponseEntity<EnterRoomResponseDto> enterRoomAndMarkRead(
            @PathVariable("roomId") Long roomId,
            HttpServletRequest request) {
        String statusCode = "5000";
        HttpStatus httpStatus = HttpStatus.OK;
        EnterRoomResponseDto responseDto = null;
        try {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            String token = authorizationHeader.replace("Bearer ", "");
            AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
            Long loginedUserId = authUserResponse.getUserId();
            responseDto = chatRoomService.enterRoomAndMarkRead(roomId, loginedUserId);
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            statusCode = "5005";
            responseDto = EnterRoomResponseDto.builder()
                    .statusCode(statusCode)
                    .statusMsg("fail")
                    .markedReadCount(0)
                    .build();
        }
        return ResponseEntity.status(httpStatus).body(responseDto);
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/leave-room/roomId={roomId}")
    public ResponseEntity<Map<String, Object>> leaveChatRoom(
            @PathVariable("roomId") Long roomId,
            HttpServletRequest request,
            @RequestHeader("Authorization") String token) {
        Map<String, Object> bodyMap = new HashMap<>();
        String statusCode = "5000";
        HttpStatus httpStatus = HttpStatus.OK;
        try {
            AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
            Long userId = authUserResponse.getUserId();
            chatRoomService.leaveRoom(roomId, userId);
            statusCode = "2000";
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            statusCode = "5005";
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        bodyMap.put("statusCode", statusCode);
        return ResponseEntity.status(httpStatus).body(bodyMap);
    }
}
