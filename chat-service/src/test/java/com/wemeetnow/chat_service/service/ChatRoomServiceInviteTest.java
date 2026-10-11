package com.wemeetnow.chat_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wemeetnow.chat_service.client.AuthServiceClient;
import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.domain.ChatParticipant;
import com.wemeetnow.chat_service.dto.AlreadyInvitedUsersResponseDto;
import com.wemeetnow.chat_service.dto.ChatParticipantPublicProfileDto;
import com.wemeetnow.chat_service.dto.CommonApiResponse;
import com.wemeetnow.chat_service.exception.ChatRoomInviteException;
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
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceInviteTest {

    private static final Long ROOM_ID = 5L;
    private static final Long INVITER_ID = 9L;
    private static final String AUTHORIZATION = "Bearer test-token";

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
    void invitesAllUsersWhenRoomMemberAndAllUsersExist() {
        List<Long> invitedIds = List.of(1L, 2L, 3L);
        when(chatRoomRepository.existsById(ROOM_ID)).thenReturn(true);
        when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, INVITER_ID)).thenReturn(true);
        invitedIds.forEach(id -> {
            when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, id)).thenReturn(false);
            when(authServiceClient.getUserById(AUTHORIZATION, id)).thenReturn(userResponse(id));
        });

        List<Long> result = chatRoomService.inviteParticipants(ROOM_ID, INVITER_ID, invitedIds, AUTHORIZATION);

        assertThat(result).containsExactlyElementsOf(invitedIds);
        verify(chatParticipantRepository).saveAll(argThat(participants -> {
            List<ChatParticipant> saved = StreamSupport.stream(participants.spliterator(), false).toList();
            return saved.stream().map(ChatParticipant::getUserId).toList().equals(invitedIds)
                    && saved.stream().allMatch(participant -> participant.getUseYn() == 'Y');
        }));
    }

    @Test
    void returnsEveryAlreadyInvitedUserWithoutPartiallyInvitingOthers() {
        List<Long> invitedIds = List.of(1L, 2L, 3L);
        when(chatRoomRepository.existsById(ROOM_ID)).thenReturn(true);
        when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, INVITER_ID)).thenReturn(true);
        when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, 1L)).thenReturn(true);
        when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, 2L)).thenReturn(false);
        when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, 3L)).thenReturn(true);

        assertThatThrownBy(() -> chatRoomService.inviteParticipants(
                ROOM_ID, INVITER_ID, invitedIds, AUTHORIZATION))
                .isInstanceOf(ChatRoomInviteException.class)
                .hasFieldOrPropertyWithValue("apiStatusCode", ApiStatusCode.CONFLICT)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT)
                .satisfies(exception -> {
                    Object data = ((ChatRoomInviteException) exception).getData();
                    assertThat(data).isInstanceOf(AlreadyInvitedUsersResponseDto.class);
                    AlreadyInvitedUsersResponseDto alreadyInvited = (AlreadyInvitedUsersResponseDto) data;
                    assertThat(alreadyInvited.getAlreadyInvitedUserIdList()).containsExactly(1L, 3L);
                    JsonNode serialized = new ObjectMapper().valueToTree(alreadyInvited);
                    assertThat(serialized.has("alreadyInvitedUserIdList")).isTrue();
                    assertThat(serialized.has("username")).isFalse();
                    assertThat(serialized.has("email")).isFalse();
                });

        verify(authServiceClient, never()).getUserById(AUTHORIZATION, 1L);
        verify(authServiceClient, never()).getUserById(AUTHORIZATION, 3L);
        verify(chatParticipantRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsRequestWhenInviteeDoesNotExistWithoutPartialInvite() {
        when(chatRoomRepository.existsById(ROOM_ID)).thenReturn(true);
        when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, INVITER_ID)).thenReturn(true);
        when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, 1L)).thenReturn(false);
        when(authServiceClient.getUserById(AUTHORIZATION, 1L)).thenReturn(null);

        assertThatThrownBy(() -> chatRoomService.inviteParticipants(ROOM_ID, INVITER_ID, List.of(1L), AUTHORIZATION))
                .isInstanceOf(ChatRoomInviteException.class)
                .hasFieldOrPropertyWithValue("apiStatusCode", ApiStatusCode.RESOURCE_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);

        verify(chatParticipantRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsInvalidRequestBody() {
        assertThatThrownBy(() -> chatRoomService.inviteParticipants(
                ROOM_ID, INVITER_ID, List.of(1L, 1L), AUTHORIZATION))
                .isInstanceOf(ChatRoomInviteException.class)
                .hasFieldOrPropertyWithValue("apiStatusCode", ApiStatusCode.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);

        verify(chatRoomRepository, never()).existsById(ROOM_ID);
        verify(chatParticipantRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsWhenChatRoomDoesNotExist() {
        when(chatRoomRepository.existsById(ROOM_ID)).thenReturn(false);

        assertThatThrownBy(() -> chatRoomService.inviteParticipants(ROOM_ID, INVITER_ID, List.of(1L), AUTHORIZATION))
                .isInstanceOf(ChatRoomInviteException.class)
                .hasFieldOrPropertyWithValue("apiStatusCode", ApiStatusCode.RESOURCE_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);

        verify(chatParticipantRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsInviterWhoIsNotRoomParticipant() {
        when(chatRoomRepository.existsById(ROOM_ID)).thenReturn(true);
        when(chatParticipantRepository.existsByChatRoomIdAndUserId(ROOM_ID, INVITER_ID)).thenReturn(false);

        assertThatThrownBy(() -> chatRoomService.inviteParticipants(ROOM_ID, INVITER_ID, List.of(1L), AUTHORIZATION))
                .isInstanceOf(ChatRoomInviteException.class)
                .hasFieldOrPropertyWithValue("apiStatusCode", ApiStatusCode.CHAT_ROOM_ACCESS_DENIED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);

        verify(authServiceClient, never()).getUserById(AUTHORIZATION, 1L);
        verify(chatParticipantRepository, never()).saveAll(anyList());
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
