package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.client.AuthServiceClient;
import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.dto.ChatParticipantPublicProfileDto;
import com.wemeetnow.chat_service.dto.CommonApiResponse;
import com.wemeetnow.chat_service.exception.ChatRoomParticipantException;
import com.wemeetnow.chat_service.repository.ChatParticipantRepository;
import com.wemeetnow.chat_service.repository.ChatReadRepository;
import com.wemeetnow.chat_service.repository.ChatRepository;
import com.wemeetnow.chat_service.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomParticipantRemovalTest {

    private static final Long ROOM_ID = 5L;
    private static final Long OWNER_ID = 9L;
    private static final String AUTHORIZATION = "Bearer valid-token";
    private static final List<Long> REMOVED_USER_IDS = List.of(1L, 2L, 3L);

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ChatReadRepository chatReadRepository;
    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private ChatParticipantRepository chatParticipantRepository;
    @Mock
    private EnterCodeGenerator enterCodeGenerator;
    @Mock
    private AuthServiceClient authServiceClient;

    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() {
        chatRoomService = new ChatRoomService(
                chatRoomRepository,
                chatRepository,
                chatReadRepository,
                restClientBuilder,
                chatParticipantRepository,
                enterCodeGenerator,
                authServiceClient
        );
    }

    @Test
    void removesAllUsersWhenRequesterOwnsRoomAndUsersExistAndAreMembers() {
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(roomOwnedBy(OWNER_ID)));
        REMOVED_USER_IDS.forEach(userId -> {
            when(authServiceClient.getUserById(AUTHORIZATION, userId)).thenReturn(userResponse(userId));
            when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, userId)).thenReturn(true);
        });

        List<Long> removed = chatRoomService.removeParticipants(
                ROOM_ID, OWNER_ID, REMOVED_USER_IDS, AUTHORIZATION);

        assertThat(removed).containsExactlyElementsOf(REMOVED_USER_IDS);
        verify(chatParticipantRepository).removeByChatRoomIdAndUserIds(ROOM_ID, REMOVED_USER_IDS);
    }

    @Test
    void rejectsRequesterWhoDidNotCreateRoom() {
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(roomOwnedBy(OWNER_ID)));

        assertThatThrownBy(() -> chatRoomService.removeParticipants(
                ROOM_ID, 10L, REMOVED_USER_IDS, AUTHORIZATION))
                .isInstanceOf(ChatRoomParticipantException.class)
                .hasFieldOrPropertyWithValue("apiStatusCode", ApiStatusCode.CHAT_ROOM_ACCESS_DENIED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);

        verify(authServiceClient, never()).getUserById(AUTHORIZATION, REMOVED_USER_IDS.get(0));
        verify(chatParticipantRepository, never()).removeByChatRoomIdAndUserIds(ROOM_ID, REMOVED_USER_IDS);
    }

    @Test
    void rejectsWhenRoomDoesNotExist() {
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.removeParticipants(
                ROOM_ID, OWNER_ID, REMOVED_USER_IDS, AUTHORIZATION))
                .isInstanceOf(ChatRoomParticipantException.class)
                .hasFieldOrPropertyWithValue("apiStatusCode", ApiStatusCode.RESOURCE_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);

        verify(chatParticipantRepository, never()).removeByChatRoomIdAndUserIds(ROOM_ID, REMOVED_USER_IDS);
    }

    @Test
    void rejectsInvalidRequest() {
        assertThatThrownBy(() -> chatRoomService.removeParticipants(
                ROOM_ID, OWNER_ID, List.of(1L, 1L), AUTHORIZATION))
                .isInstanceOf(ChatRoomParticipantException.class)
                .hasFieldOrPropertyWithValue("apiStatusCode", ApiStatusCode.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);

        verify(chatRoomRepository, never()).findById(ROOM_ID);
        verify(chatParticipantRepository, never()).removeByChatRoomIdAndUserIds(ROOM_ID, REMOVED_USER_IDS);
    }

    @Test
    void rejectsRequesterIncludedInRemovalListAsInvalidRequest() {
        assertThatThrownBy(() -> chatRoomService.removeParticipants(
                ROOM_ID, OWNER_ID, List.of(1L, OWNER_ID), AUTHORIZATION))
                .isInstanceOf(ChatRoomParticipantException.class)
                .hasFieldOrPropertyWithValue("apiStatusCode", ApiStatusCode.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST)
                .hasMessage("요청자 본인은 퇴장 대상에 포함할 수 없습니다. removedUserIdList를 확인해 주세요.");

        verify(chatRoomRepository, never()).findById(ROOM_ID);
        verify(authServiceClient, never()).getUserById(AUTHORIZATION, OWNER_ID);
        verify(chatParticipantRepository, never())
                .removeByChatRoomIdAndUserIds(ROOM_ID, REMOVED_USER_IDS);
    }

    @Test
    void rejectsWhenAnyTargetUserDoesNotExist() {
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(roomOwnedBy(OWNER_ID)));
        when(authServiceClient.getUserById(AUTHORIZATION, 1L)).thenReturn(userResponse(1L));
        when(authServiceClient.getUserById(AUTHORIZATION, 2L)).thenReturn(null);

        assertThatThrownBy(() -> chatRoomService.removeParticipants(
                ROOM_ID, OWNER_ID, REMOVED_USER_IDS, AUTHORIZATION))
                .isInstanceOf(ChatRoomParticipantException.class)
                .hasFieldOrPropertyWithValue("apiStatusCode", ApiStatusCode.RESOURCE_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);

        verify(chatParticipantRepository, never())
                .removeByChatRoomIdAndUserIds(ROOM_ID, REMOVED_USER_IDS);
    }

    @Test
    void rejectsWhenAnyTargetUserIsNotAnActiveRoomMember() {
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(roomOwnedBy(OWNER_ID)));
        REMOVED_USER_IDS.forEach(userId ->
                when(authServiceClient.getUserById(AUTHORIZATION, userId)).thenReturn(userResponse(userId)));
        when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, 1L)).thenReturn(true);
        when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, 2L)).thenReturn(false);
        when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, 3L)).thenReturn(true);

        assertThatThrownBy(() -> chatRoomService.removeParticipants(
                ROOM_ID, OWNER_ID, REMOVED_USER_IDS, AUTHORIZATION))
                .isInstanceOf(ChatRoomParticipantException.class)
                .hasFieldOrPropertyWithValue("apiStatusCode", ApiStatusCode.RESOURCE_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);

        verify(chatParticipantRepository, never())
                .removeByChatRoomIdAndUserIds(ROOM_ID, REMOVED_USER_IDS);
    }

    private ChatRoom roomOwnedBy(Long userId) {
        return ChatRoom.builder()
                .chatRoomNm("room")
                .inpUserId(String.valueOf(userId))
                .build();
    }

    private CommonApiResponse<ChatParticipantPublicProfileDto> userResponse(Long userId) {
        ChatParticipantPublicProfileDto user = ChatParticipantPublicProfileDto.builder()
                .userId(userId)
                .username("user-" + userId)
                .build();
        return CommonApiResponse.<ChatParticipantPublicProfileDto>builder()
                .statusCode(ApiStatusCode.SUCCESS.getCode())
                .data(user)
                .build();
    }
}
