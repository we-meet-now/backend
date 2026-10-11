package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatParticipant;
import com.wemeetnow.chat_service.domain.ChatRead;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.client.AuthServiceClient;
import com.wemeetnow.chat_service.dto.*;
import com.wemeetnow.chat_service.exception.ChatRoomInviteException;
import com.wemeetnow.chat_service.repository.ChatParticipantRepository;
import com.wemeetnow.chat_service.repository.ChatReadRepository;
import com.wemeetnow.chat_service.repository.ChatRepository;
import com.wemeetnow.chat_service.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import feign.FeignException;

import java.time.LocalDateTime;
import java.util.HashSet;
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
    private final AuthServiceClient authServiceClient;

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
        String statusCode = ApiStatusCode.SUCCESS.getCode();
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
            statusCode = ApiStatusCode.INTERNAL_SERVER_ERROR.getCode();
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

    public GuestYnDto isGuestToken(String token) {
        try {
            String jwtHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

            RestClient restClient = restClientBuilder
                    .baseUrl(AUTH_SERVICE_URL)
                    .build();

            // Auth Service 호출: CommonApiResponse 형태로 응답받음
            CommonApiResponse<GuestYnDto> response = restClient.post()
                    .uri("/api/auth/v1/users/no-login/is-guest-token")
                    .header(HttpHeaders.AUTHORIZATION, jwtHeader) // Bearer 토큰 전달
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new GuestCheckRequestDto(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        // 401 Unauthorized 등 처리
                        throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
                    })
                    .body(new org.springframework.core.ParameterizedTypeReference<CommonApiResponse<GuestYnDto>>() {});

            if (response != null) {
                log.info("Successfully fetched guest Y/N from auth-service");
                return response.getData();
            }
            return null;
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            return null;
        }

    }

    public boolean isMember(Long roomId, Long userId) {
        return chatParticipantsRepository.existsByChatRoomIdAndUserId(roomId, userId);
    }

    @Transactional
    public List<Long> inviteParticipants(
            Long roomId, Long inviterUserId, List<Long> invitedUserIdList, String authorizationHeader) {
        if (roomId == null || roomId <= 0 || invitedUserIdList == null || invitedUserIdList.isEmpty()
                || invitedUserIdList.stream().anyMatch(userId -> userId == null || userId <= 0)
                || new HashSet<>(invitedUserIdList).size() != invitedUserIdList.size()) {
            throw inviteException(ApiStatusCode.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "요청 본문의 roomId 또는 invitedUserIdList가 유효하지 않습니다.");
        }

        if (!chatRoomRepository.existsById(roomId)) {
            throw inviteException(ApiStatusCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND,
                    "채팅방을 찾을 수 없습니다. roomId=" + roomId);
        }

        if (!isMember(roomId, inviterUserId)) {
            throw inviteException(ApiStatusCode.CHAT_ROOM_ACCESS_DENIED, HttpStatus.FORBIDDEN,
                    "채팅방 참여자만 새 사용자를 초대할 수 있습니다.");
        }

        List<Long> alreadyInvitedUserIds = invitedUserIdList.stream()
                .filter(invitedUserId -> isMember(roomId, invitedUserId))
                .toList();
        if (!alreadyInvitedUserIds.isEmpty()) {
            throw inviteException(
                    ApiStatusCode.CONFLICT,
                    HttpStatus.CONFLICT,
                    "초대 목록에 이미 채팅방에 참여 중인 사용자가 포함되어 있습니다.",
                    AlreadyInvitedUsersResponseDto.builder()
                            .alreadyInvitedUserIdList(alreadyInvitedUserIds)
                            .build()
            );
        }

        for (Long invitedUserId : invitedUserIdList) {
            try {
                CommonApiResponse<ChatParticipantPublicProfileDto> userResponse =
                        authServiceClient.getUserById(authorizationHeader, invitedUserId);
                ChatParticipantPublicProfileDto user = userResponse != null ? userResponse.getData() : null;
                if (user == null || !invitedUserId.equals(user.getUserId())) {
                    throw userNotFound(invitedUserId);
                }
            } catch (FeignException.NotFound e) {
                throw userNotFound(invitedUserId);
            }
        }

        List<ChatParticipant> participants = invitedUserIdList.stream()
                .map(userId -> ChatParticipant.builder()
                        .chatRoomId(roomId)
                        .userId(userId)
                        .useYn('Y')
                        .build())
                .toList();
        chatParticipantsRepository.saveAll(participants);
        return List.copyOf(invitedUserIdList);
    }

    private ChatRoomInviteException userNotFound(Long userId) {
        return inviteException(ApiStatusCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND,
                "초대할 사용자를 찾을 수 없습니다.");
    }

    private ChatRoomInviteException inviteException(ApiStatusCode code, HttpStatus status, String message) {
        return new ChatRoomInviteException(code, status, message);
    }

    private ChatRoomInviteException inviteException(
            ApiStatusCode code, HttpStatus status, String message, Object data) {
        return new ChatRoomInviteException(code, status, message, data);
    }

    @Transactional
    public void addParticipant(Long roomId, Long userId) {
        if (!isMember(roomId, userId)) {
            ChatParticipant chatParticipant = ChatParticipant.builder()
                    .chatRoomId(roomId)
                    .userId(userId)
                    .useYn('Y')
                    .build();
            chatParticipantsRepository.save(chatParticipant);
        }
    }
}
