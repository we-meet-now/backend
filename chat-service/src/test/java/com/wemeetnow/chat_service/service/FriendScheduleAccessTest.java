package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.client.AuthServiceClient;
import com.wemeetnow.chat_service.domain.UserSchedule;
import com.wemeetnow.chat_service.domain.UserScheduleParticipant;
import com.wemeetnow.chat_service.domain.enums.UserScheduleStatus;
import com.wemeetnow.chat_service.dto.AuthFriendInfoDto;
import com.wemeetnow.chat_service.dto.CommonApiResponse;
import com.wemeetnow.chat_service.exception.ScheduleAccessDeniedException;
import com.wemeetnow.chat_service.repository.UserScheduleParticipantRepository;
import com.wemeetnow.chat_service.repository.UserScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendScheduleAccessTest {

    private static final Long OWNER_ID = 10L;
    private static final Long REQUESTER_ID = 20L;
    private static final Long SCHEDULE_ID = 30L;
    private static final Long OWNER_PARTICIPANT_ID = 40L;
    private static final String AUTH_HEADER = "Bearer sample-token";

    @Mock
    private UserScheduleRepository userScheduleRepository;

    @Mock
    private UserScheduleParticipantRepository participantRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private RestClient.Builder restClientBuilder;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void returnsOnlyOpenSchedulesWhenCallerHasAcceptedFriendStatus() {
        UserSchedule schedule = UserSchedule.builder()
                .userScheduleId(SCHEDULE_ID)
                .title("Open event")
                .startAt(LocalDateTime.of(2026, 10, 12, 10, 0))
                .endAt(LocalDateTime.of(2026, 10, 12, 11, 0))
                .status(UserScheduleStatus.CONFIRMED)
                .publicScheduleYn("Y")
                .publicMemberYn("N")
                .build();
        when(authServiceClient.getFriendList(AUTH_HEADER)).thenReturn(friendResponse(OWNER_ID, "HIDE"));
        when(userScheduleRepository.findPublicSchedulesByOwnerBetween(
                eq(OWNER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(schedule));

        var result = scheduleService.getFriendSchedules(OWNER_ID, REQUESTER_ID, AUTH_HEADER, "202610");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Open event");
        assertThat(result.get(0).getPublicMemberYn()).isEqualTo("N");
    }

    @Test
    void rejectsPendingFriendAndDoesNotQuerySchedules() {
        when(authServiceClient.getFriendList(AUTH_HEADER)).thenReturn(friendResponse(OWNER_ID, "WAIT"));

        assertThatThrownBy(() -> scheduleService.getFriendSchedules(
                OWNER_ID, REQUESTER_ID, AUTH_HEADER, "202610"))
                .isInstanceOf(ScheduleAccessDeniedException.class);
        verifyNoInteractions(userScheduleRepository);
    }

    @Test
    void rejectsNonFriendRequestForOpenScheduleDetail() {
        UserSchedule schedule = UserSchedule.builder()
                .userScheduleId(SCHEDULE_ID)
                .title("Private to friends")
                .startAt(LocalDateTime.of(2026, 10, 12, 10, 0))
                .status(UserScheduleStatus.PENDING)
                .publicScheduleYn("Y")
                .userScheduleParticipantId(OWNER_PARTICIPANT_ID)
                .build();
        UserScheduleParticipant owner = UserScheduleParticipant.builder()
                .userScheduleParticipantId(OWNER_PARTICIPANT_ID)
                .userId(OWNER_ID)
                .userScheduleId(SCHEDULE_ID)
                .build();
        when(userScheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(participantRepository.existsByUserScheduleIdAndUserId(SCHEDULE_ID, REQUESTER_ID))
                .thenReturn(false);
        when(participantRepository.findById(OWNER_PARTICIPANT_ID)).thenReturn(Optional.of(owner));
        when(authServiceClient.getFriendList(AUTH_HEADER)).thenReturn(friendResponse(OWNER_ID, "BLOCK"));

        assertThatThrownBy(() -> scheduleService.getScheduleDetail(SCHEDULE_ID, REQUESTER_ID, AUTH_HEADER))
                .isInstanceOf(ScheduleAccessDeniedException.class);
    }

    private CommonApiResponse<List<AuthFriendInfoDto>> friendResponse(Long friendId, String status) {
        AuthFriendInfoDto friend = new AuthFriendInfoDto();
        friend.setFriendId(friendId);
        friend.setFriendStatus(status);
        return CommonApiResponse.<List<AuthFriendInfoDto>>builder()
                .data(List.of(friend))
                .build();
    }
}
