package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatParticipant;
import com.wemeetnow.chat_service.domain.ChatRead;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.dto.AuthUserDto;
import com.wemeetnow.chat_service.dto.CommonApiResponse;
import com.wemeetnow.chat_service.dto.CreateAnonymousChatRoomRequestDto;
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
    private final EnterCodeGenerator enterCodeGenerator;

    /**
     * 채팅방 생성 및 참여코드 생성(참여자 정보 없음)
     */
    @Transactional
    public ChatRoom createChatRoom(String inpUserId, String chatRoomNm) {
        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomNm(chatRoomNm)
                .placeId(null)
                .meetTime(null)
                .meetType(null)
                .inpUserId(inpUserId)
                .build();
        chatRoomRepository.save(chatRoom);
        
        String enterCode = enterCodeGenerator.generateEnterCode(chatRoom.getChatRoomId());
        chatRoom.setEnterCode(enterCode);
        chatRoomRepository.save(chatRoom);

        // 자기 자신만 참여자로 저장
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .userId(Long.parseLong(inpUserId))
                .useYn('Y')
                .build();
        chatParticipantsRepository.save(chatParticipant);

        return chatRoom;
    }

    /**
     * 채팅방 생성 및 참여코드 생성(참여자 정보 저장)
     */
    @Transactional
    public ChatRoom createChatRoomWithParticipants(String inpUserId, String chatRoomNm, List<Long> participantIds) {
        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomNm(chatRoomNm)
                .placeId(null)
                .meetTime(null)
                .meetType(null)
                .inpUserId(inpUserId)
                .build();
        chatRoomRepository.save(chatRoom);

        String enterCode = enterCodeGenerator.generateEnterCode(chatRoom.getChatRoomId());
        chatRoom.setEnterCode(enterCode);
        chatRoomRepository.save(chatRoom);

        for (Long userId : participantIds) {
            ChatParticipant chatParticipant = ChatParticipant.builder()
                    .chatRoomId(chatRoom.getChatRoomId())
                    .userId(userId)
                    .useYn('Y')
                    .build();
            chatParticipantsRepository.save(chatParticipant);
        }
        return chatRoom;
    }

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

            // Auth Service 호출: CommonApiResponse 형태로 응답받음
            CommonApiResponse<AuthUserDto> response = restClient.get()
                    .uri("/api/auth/v1/users/get-id")
                    .header("Authorization", jwtHeader)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<CommonApiResponse<AuthUserDto>>() {});
            
            if (response != null) {
                log.info("Successfully fetched user from auth-service: userId={}", response.getData().getUserId());
                return response.getData();
            }
            return null;
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            return null;
        }

    }

    public ChatUserInfo fetchUserInfoFromAuthService(String token) {
        try {
            String jwtHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

            RestClient restClient = restClientBuilder
                    .baseUrl(AUTH_SERVICE_URL)
                    .build();

            // Auth Service 호출: CommonApiResponse 형태로 응답받음
            CommonApiResponse<ChatUserInfo> response = restClient.get()
                    .uri("/api/auth/v1/users/get-user-info")
                    .header("Authorization", jwtHeader)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<CommonApiResponse<ChatUserInfo>>() {});
            
            if (response != null) {
                log.info("Successfully fetched user info from auth-service");
                return response.getData();
            }
            return null;
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
    public ChatRoom createAnonymousChatRoom(Long userId, String chatRoomNm, CreateAnonymousChatRoomRequestDto requestDto) {
        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomNm(chatRoomNm)
                .placeId((long) requestDto.getPlaceId())
                .meetTime(requestDto.getMeetTime())
                .meetType(requestDto.getMeetType())
                .inpUserId(String.valueOf(userId))
                .build();
        chatRoomRepository.save(chatRoom);
        
        String enterCode = enterCodeGenerator.generateEnterCode(chatRoom.getChatRoomId());
        chatRoom.setEnterCode(enterCode);
        chatRoomRepository.save(chatRoom);

        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .userId(userId)
                .useYn('Y')
                .build();
        chatParticipantsRepository.save(chatParticipant);

        return chatRoom;
    }

    @Transactional
    public ChatGuestTokenInfo generateGuestToken() {
        try {
            RestClient restClient = restClientBuilder
                    .baseUrl(AUTH_SERVICE_URL)
                    .build();

            // Auth Service 호출: CommonApiResponse 형태로 응답받음
            CommonApiResponse<ChatGuestTokenInfo> response = restClient.get()
                    .uri("/api/auth/v1/users/no-login/get-guest-token")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<CommonApiResponse<ChatGuestTokenInfo>>() {});


            if (response != null) {
                log.info("Successfully fetched guest Token info from auth-service");
                return response.getData();
            }
            return null;
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            return null;
        }
    }
}
