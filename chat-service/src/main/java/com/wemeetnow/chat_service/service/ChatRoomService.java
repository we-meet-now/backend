package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatParticipant;
import com.wemeetnow.chat_service.domain.ChatRead;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.dto.AuthUserDto;
import com.wemeetnow.chat_service.dto.EnterRoomResponseDto;
import com.wemeetnow.chat_service.repository.ChatParticipantRepository;
import com.wemeetnow.chat_service.repository.ChatReadRepository;
import com.wemeetnow.chat_service.repository.ChatRepository;
import com.wemeetnow.chat_service.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;
    private final ChatReadRepository chatReadRepository;
    private final RestClient.Builder restClientBuilder;
    private final ChatParticipantRepository chatParticipantsRepository;

    @Value("${external.auth-service.url}")
    private String AUTH_SERVICE_URL;

    public List<ChatRoom> findByUserId(Long userId) {
        return chatRoomRepository.findByUserId(userId);
    }

    public AuthUserDto fetchUserFromAuthService(String token) {
        try {
            // "Bearer " 접두사가 중복되지 않도록 처리
            String jwtHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

            RestClient restClient = restClientBuilder
                    .baseUrl(AUTH_SERVICE_URL)
                    .build();

            // Auth Service 호출: 6112 포트로 요청
            return restClient.get()
                    .uri("/api/v1/users/get-id")
                    .header("Authorization", jwtHeader)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(AuthUserDto.class);
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            return null;
        }

    }
    public EnterRoomResponseDto enterRoomAndMarkRead(Long roomId, Long userId) {
        String statusCode = "2000";
        String statusMsg = "success";
        int markedCount = 0;
        try {
            List<Chat> unreadChats = chatRepository.findUnreadChatsForUser(roomId, userId, LocalDateTime.now());
            for (Chat chat : unreadChats) {
                if (!chatReadRepository.existsByChatIdAndUserId(chat.getChatId(), userId)) {
                    ChatRead chatRead = new ChatRead(chat.getChatId(), userId);
                    chatReadRepository.save(chatRead);
                    markedCount++;
                }
            }
        } catch (Exception e) {
            log.error("Error in enterRoomAndMarkRead: {}", e.getMessage());
            statusCode = "5005";
            statusMsg = "fail";
            markedCount = 0;
        }
        return new EnterRoomResponseDto(statusCode, statusMsg, markedCount);
    }
    // ChatRoomService.java
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        chatParticipantsRepository.deleteByChatRoomIdAndUserId(roomId, userId);
    }

    @Transactional
    public Long createAnonymousChatRoom(Long userId, String chatRoomNm) {
        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomNm(chatRoomNm)
                .inpUserId(String.valueOf(userId))
                .build();
        chatRoomRepository.save(chatRoom);

        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .userId(userId)
                .useYn('Y')
                .build();
        chatParticipantsRepository.save(chatParticipant);

        return chatRoom.getChatRoomId();
    }
}
