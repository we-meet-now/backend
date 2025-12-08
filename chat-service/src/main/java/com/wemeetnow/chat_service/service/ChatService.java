package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.domain.enums.ChatType;
import com.wemeetnow.chat_service.dto.AuthUserResponse;
import com.wemeetnow.chat_service.dto.ChatResponseDto;
import com.wemeetnow.chat_service.repository.ChatRepository;
import com.wemeetnow.chat_service.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RestClient.Builder restClientBuilder;
    @Value("${auth-service.url}")
    private String authServiceUrl;

    @Transactional
    public ChatRoom createRoom(String name) {
        ChatRoom room = ChatRoom.builder()
                .chatRoomNm(name)
                .build();
        return chatRoomRepository.save(room);
    }

    public ChatRoom getRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다. id=" + roomId));
    }

    @Transactional
    public ChatResponseDto saveMessage(Long roomId, Long userId, String message, ChatType chatType) throws Exception {
        Chat savedChat = null;
        try {
            Chat chat = Chat.builder()
                    .chatRoomId(roomId)
                    .userId(userId)
                    .message(message)
                    .chatType(chatType)
                    .notReadCount(0)
                    .build();
            savedChat = chatRepository.save(chat); // 여기서 inpDate 등이 자동 생성됨
            return ChatResponseDto.fromEntity(savedChat);
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            throw new Exception();
        }
        // 엔티티가 아닌 DTO를 반환

    }

    public List<Chat> getChatList(Long roomId) {
        return chatRepository.findByChatRoomId(roomId);
    }

    public AuthUserResponse isValidAccessToken(String token) {
        String authorizationHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        log.info("auth-service.url: {}", authServiceUrl);
        log.info("authorizationHeader: {}", authorizationHeader);
        RestClient restClient = restClientBuilder.baseUrl(authServiceUrl).build();

        return restClient.get()
                .uri("/api/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader) // Bearer 토큰 전달
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), (req, res) -> {
                    // 401 Unauthorized 등 처리
                    throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
                })
                .body(AuthUserResponse.class);
    }
}
