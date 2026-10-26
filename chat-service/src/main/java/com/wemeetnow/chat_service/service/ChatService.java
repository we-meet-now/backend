package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.client.AuthServiceClient;
import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.domain.enums.ChatType;
import com.wemeetnow.chat_service.dto.AuthGuestResponse;
import com.wemeetnow.chat_service.dto.AuthUserDto;
import com.wemeetnow.chat_service.dto.AuthUserResponse;
import com.wemeetnow.chat_service.dto.ChatParticipantPublicProfileDto;
import com.wemeetnow.chat_service.dto.ChatResponseDto;
import com.wemeetnow.chat_service.dto.CommonApiResponse;
import com.wemeetnow.chat_service.dto.GuestIdRequestDto;
import com.wemeetnow.chat_service.dto.GuestUserDto;
import com.wemeetnow.chat_service.repository.ChatRepository;
import com.wemeetnow.chat_service.repository.ChatRoomRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RestClient.Builder restClientBuilder;
    private final CommonService commonService;
    private final AuthServiceClient authServiceClient;
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
    public ChatResponseDto saveMessage(
            Long roomId, Long userId, String message, ChatType chatType, String username) throws Exception {
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
            return ChatResponseDto.fromEntity(savedChat, username);
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            throw new Exception();
        }
        // 엔티티가 아닌 DTO를 반환

    }

    public List<Chat> getChatList(Long roomId, Long userId) {
        return chatRepository.findByChatRoomIdAndUserId(roomId, userId);
    }

    public List<ChatResponseDto> getChatResponseList(List<Chat> chats, String authorizationHeader) {
        Map<Long, String> usernamesByUserId = new HashMap<>();
        chats.stream()
                .map(Chat::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(userId -> usernamesByUserId.put(userId, getUsername(userId, authorizationHeader)));

        return chats.stream()
                .map(chat -> ChatResponseDto.fromEntity(chat, usernamesByUserId.get(chat.getUserId())))
                .toList();
    }

    public String getUsername(Long userId, String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization 토큰이 필요합니다.");
        }
        String bearerToken = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader
                : "Bearer " + authorizationHeader;

        try {
            CommonApiResponse<ChatParticipantPublicProfileDto> response =
                    authServiceClient.getUserById(bearerToken, userId);
            ChatParticipantPublicProfileDto profile = response != null ? response.getData() : null;
            if (response == null
                    || !ApiStatusCode.SUCCESS.getCode().equals(response.getStatusCode())
                    || profile == null
                    || !userId.equals(profile.getUserId())) {
                throw new IllegalStateException("Auth 서비스에서 유효한 사용자 프로필을 받지 못했습니다.");
            }
            return profile.getUsername();
        } catch (FeignException.NotFound e) {
            return null;
        }
    }

    public List<Chat> getAllChats() {
        return chatRepository.findAll();
    }

    public AuthUserResponse isValidAccessToken(String token) {
        AuthUserDto authUserDto = commonService.fetchUserFromAuthService(token);
        AuthUserResponse authUserResponse = new AuthUserResponse();
        authUserResponse.setUserId(authUserDto.getUserId());
        return authUserResponse;
    }

    public AuthGuestResponse isValidGuestToken(String token, String uuId) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("게스트 토큰이 필요합니다.");
        }

        String authorizationHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        RestClient restClient = restClientBuilder.baseUrl(authServiceUrl).build();
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

    public AuthGuestResponse isValidGuestToken(String token) {
        return isValidGuestToken(token, null);
    }

    public int getNotReadCountByRoomIdAndUserId(Long roomId, Long userId) {
        return chatRepository.countNotReadByRoomIdAndUserId(roomId, userId);
    }
}
