package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.domain.ChatParticipant;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.service.ChatParticipantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ChatParticipantService chatParticipantService;

    // TODO 지금 CP가 return 되고 있는대 이것을 User 정보로 return 하는 방법? -> Repository에서 User 접근이 안되는게 문제임(User클래스 절대경로 다 명시할까?)
    @GetMapping("/roomId={roomId}")
    public ResponseEntity getChatParticipants(@PathVariable("roomId") Long roomId, HttpServletRequest request) {
        log.info("request: {}", request);
        HttpStatus httpStatus = HttpStatus.OK;
        Long loginedUserId = 0L;
        String statusCode = "5000";
        Map<String, Object> bodyMap = new HashMap<>();
        List<ChatParticipant> chatParticipantList = new ArrayList<>();
        try {
//            String accessToken = jwtUtil.getAccessTokenFromHeader(request);
//            if (!JwtUtil.isExpired(accessToken)) {
//                loginedUserId = JwtUtil.getId(accessToken);
//                statusCode = "2000";
//                chatRoomList = chatRoomService.findByUserId(loginedUserId);
//            }
            loginedUserId = 1L;
            statusCode = "2000";
            chatParticipantList = chatParticipantService.findByChatRoomId(roomId);

        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            statusCode = "5005";
        } finally {
            bodyMap.put("statusCode", statusCode);
            bodyMap.put("loginedUserId", loginedUserId);
            bodyMap.put("chatParticipantList", chatParticipantList);
        }
        return ResponseEntity.status(httpStatus).body(bodyMap);
    }
}
