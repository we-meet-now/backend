package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.domain.enums.ChatType;
import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.dto.AuthGuestResponse;
import com.wemeetnow.chat_service.dto.AuthUserResponse;
import com.wemeetnow.chat_service.dto.ChatResponseDto;
import com.wemeetnow.chat_service.dto.CommonApiResponse;
import com.wemeetnow.chat_service.dto.GuestIdRequestDto;
import com.wemeetnow.chat_service.dto.GuestUserDto;
import com.wemeetnow.chat_service.repository.ChatRepository;
import com.wemeetnow.chat_service.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

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

    public List<Chat> getChatList(Long roomId, Long userId) {
        return chatRepository.findByChatRoomIdAndUserId(roomId, userId);
    }

    public AuthUserResponse isValidAccessToken(String token) {
        String authorizationHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        log.info("auth-service.url: {}", authServiceUrl);
        RestClient restClient = restClientBuilder.baseUrl(authServiceUrl).build();

        CommonApiResponse<AuthUserResponse> response = restClient.get()
                .uri("/api/auth/v1/users/get-id")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader) // Bearer 토큰 전달
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    // 401 Unauthorized 등 처리
                    throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
                })
                .body(new ParameterizedTypeReference<>() {});
        AuthUserResponse authUserResponse = response != null ? response.getData() : null;
        if (authUserResponse == null || authUserResponse.getUserId() == null) {
            throw new IllegalStateException("Auth service did not return a user ID.");
        }
        return authUserResponse;
    }

    public AuthGuestResponse isValidGuestToken(String token, String uuId) {
        String authorizationHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        log.info("auth-service.url: {}", authServiceUrl);
        log.info("authorizationHeader: {}", authorizationHeader);
        log.info("requestDto.uuId: {}", uuId);
        RestClient restClient = restClientBuilder.baseUrl(authServiceUrl).build();
        // 4xx/5xx 여부와 관계없이 본문을 그대로 읽어서 실패 사유를 정확히 로그로 남긴다.
        CommonApiResponse<GuestUserDto> response = restClient.post()
                .uri("/api/auth/v1/users/get-guest-id")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GuestIdRequestDto(uuId))
                .accept(MediaType.APPLICATION_JSON)
                .exchange((req, res) -> res.bodyTo(new org.springframework.core.ParameterizedTypeReference<CommonApiResponse<GuestUserDto>>() {}));

        GuestUserDto data = response != null ? response.getData() : null;
        if (data == null || !ApiStatusCode.SUCCESS.getCode().equals(data.getStatusCd())) {
            String reason = data != null ? data.getStatusMsg() : (response != null ? response.getMessage() : "auth-service 응답이 비어있습니다.");
            log.warn("게스트 토큰 검증 실패: reason={}", reason);
            throw new IllegalArgumentException(reason != null ? reason : "유효하지 않은 토큰이거나 존재하지 않는 UUID입니다.");
        }

        return AuthGuestResponse.builder()
                .userId(data.getUserId())
                .uuId(data.getUuId())
                .guestToken(data.getGuestToken())
                .isRefreshed(data.getIsRefreshed())
                .build();
    }

    public int getNotReadCountByRoomIdAndUserId(Long roomId, Long userId) {
        return chatRepository.countNotReadByRoomIdAndUserId(roomId, userId);
    }
}
