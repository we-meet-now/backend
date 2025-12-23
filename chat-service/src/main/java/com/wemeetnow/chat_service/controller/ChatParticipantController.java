package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.client.AuthServiceClient;
import com.wemeetnow.chat_service.domain.ChatParticipant;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.dto.AuthUserDto;
import com.wemeetnow.chat_service.dto.ChatParticipantUserDto;
import com.wemeetnow.chat_service.service.ChatParticipantService;
import com.wemeetnow.chat_service.service.ChatRoomService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat-participants")
public class ChatParticipantController {
    private final ChatRoomService chatRoomService;
    private final ChatParticipantService chatParticipantService;
    private final AuthServiceClient authServiceClient;

    @GetMapping("/roomId={roomId}")
    public ResponseEntity getChatParticipants(@PathVariable("roomId") Long roomId, HttpServletRequest request) {
        log.info("request: {}", request);
        HttpStatus httpStatus = HttpStatus.OK;
        Long loginedUserId = 0L;
        String statusCode = "5000";
        Map<String, Object> bodyMap = new HashMap<>();
        List<Long> userIdList = new ArrayList<>();
        List<ChatParticipantUserDto> userInfoList = new ArrayList<>();
        try {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            String accessToken = authorizationHeader.replace("Bearer ", "");
            AuthUserDto authUserDto = chatRoomService.fetchUserFromAuthService(accessToken);
            statusCode = "2000";
            userIdList = chatParticipantService.findByChatRoomId(roomId, loginedUserId);

            // 각 userId에 대해 사용자 정보 조회
            for (Long userId : userIdList) {
                ChatParticipantUserDto userInfo = authServiceClient.getUserById(userId);
                userInfoList.add(userInfo);
            }
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            statusCode = "5005";
        } finally {
            bodyMap.put("statusCode", statusCode);
            bodyMap.put("loginedUserId", loginedUserId);
            bodyMap.put("userIdList", userIdList);
            bodyMap.put("userInfoList", userInfoList);
        }
        return ResponseEntity.status(httpStatus).body(bodyMap);
    }
    @GetMapping("/anonymous-chat-roomId={roomId}")
    public ResponseEntity getAnonymousChatParticipants(@PathVariable("roomId") Long roomId) {
        HttpStatus httpStatus = HttpStatus.OK;
        Long loginedUserId = 0L;
        String statusCode = "5000";
        Map<String, Object> bodyMap = new HashMap<>();
        List<ChatParticipant> chatParticipantList = new ArrayList<>();
        List<ChatParticipantUserDto> userInfoList = new ArrayList<>();
        try {
            statusCode = "2000";
            chatParticipantList = chatParticipantService.findByAnonymousChatRoomId(roomId);

            // 각 userId에 대해 사용자 정보 조회
            for (ChatParticipant chatParticipant : chatParticipantList) {
                ChatParticipantUserDto userInfo = authServiceClient.getUserById(chatParticipant.getUserId());
                userInfoList.add(userInfo);
            }
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            statusCode = "5005";
        } finally {
            bodyMap.put("statusCode", statusCode);
            bodyMap.put("loginedUserId", loginedUserId);
            bodyMap.put("chatParticipantList", chatParticipantList);
            bodyMap.put("userInfoList", userInfoList);
        }
        return ResponseEntity.status(httpStatus).body(bodyMap);
    }
}
