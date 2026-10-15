package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.domain.Schedule;
import com.wemeetnow.chat_service.dto.RoomScheduleRequestDto;
import com.wemeetnow.chat_service.exception.InvalidScheduleRequestException;
import com.wemeetnow.chat_service.exception.ScheduleAccessDeniedException;
import com.wemeetnow.chat_service.exception.ScheduleNotFoundException;
import com.wemeetnow.chat_service.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomScheduleServiceTest {

    private static final Long ROOM_ID = 12L;
    private static final Long USER_ID = 34L;
    private static final Long SCHEDULE_ID = 56L;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ChatRoomService chatRoomService;

    @InjectMocks
    private RoomScheduleService roomScheduleService;

    @BeforeEach
    void setUp() {
        when(chatRoomService.isMember(ROOM_ID, USER_ID)).thenReturn(true);
    }

    @Test
    void listsActiveSchedulesForRoomMember() {
        Schedule schedule = schedule("Y");
        when(scheduleRepository.findActiveByRoomId(ROOM_ID)).thenReturn(List.of(schedule));

        var result = roomScheduleService.getSchedules(ROOM_ID, USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduleId()).isEqualTo(SCHEDULE_ID);
        verify(scheduleRepository).findActiveByRoomId(ROOM_ID);
    }

    @Test
    void createsScheduleWithAuthenticatedUserAndActiveStatus() {
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            schedule.setScheduleId(SCHEDULE_ID);
            return schedule;
        });

        var result = roomScheduleService.createSchedule(ROOM_ID, USER_ID, request());

        assertThat(result.getScheduleId()).isEqualTo(SCHEDULE_ID);
        assertThat(result.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getUseYn()).isEqualTo("Y");
        verify(scheduleRepository).save(argThat(schedule ->
                ROOM_ID.equals(schedule.getRoomId()) && USER_ID.equals(schedule.getUserId())));
    }

    @Test
    void readsScheduleOnlyFromRequestedRoom() {
        when(scheduleRepository.findActiveById(SCHEDULE_ID)).thenReturn(Optional.of(schedule("Y")));

        var result = roomScheduleService.getSchedule(ROOM_ID, SCHEDULE_ID, USER_ID);

        assertThat(result.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(result.getScheduleId()).isEqualTo(SCHEDULE_ID);
    }

    @Test
    void updatesScheduleFieldsWithoutChangingRoomOrOwner() {
        Schedule existing = schedule("Y");
        when(scheduleRepository.findActiveById(SCHEDULE_ID)).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RoomScheduleRequestDto update = request();
        update.setMeetingType("DINNER");

        var result = roomScheduleService.updateSchedule(ROOM_ID, SCHEDULE_ID, USER_ID, update);

        assertThat(result.getMeetingType()).isEqualTo("DINNER");
        assertThat(result.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void softDeletesScheduleAndDoesNotRemoveDatabaseRow() {
        Schedule existing = schedule("Y");
        when(scheduleRepository.findActiveById(SCHEDULE_ID)).thenReturn(Optional.of(existing));

        roomScheduleService.deleteSchedule(ROOM_ID, SCHEDULE_ID, USER_ID);

        verify(scheduleRepository).save(argThat(schedule -> "N".equals(schedule.getUseYn())));
        verify(scheduleRepository, never()).delete(any(Schedule.class));
    }

    @Test
    void deniesNonMemberAndDoesNotQueryScheduleData() {
        when(chatRoomService.isMember(ROOM_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> roomScheduleService.getSchedules(ROOM_ID, USER_ID))
                .isInstanceOf(ScheduleAccessDeniedException.class);
        verifyNoInteractions(scheduleRepository);
    }

    @Test
    void rejectsScheduleFromAnotherRoomAndMissingSchedule() {
        Schedule otherRoomSchedule = schedule("Y");
        otherRoomSchedule.setRoomId(99L);
        when(scheduleRepository.findActiveById(SCHEDULE_ID)).thenReturn(Optional.of(otherRoomSchedule));

        assertThatThrownBy(() -> roomScheduleService.getSchedule(ROOM_ID, SCHEDULE_ID, USER_ID))
                .isInstanceOf(ScheduleNotFoundException.class);

        when(scheduleRepository.findActiveById(SCHEDULE_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomScheduleService.getSchedule(ROOM_ID, SCHEDULE_ID, USER_ID))
                .isInstanceOf(ScheduleNotFoundException.class);
    }

    @Test
    void rejectsFieldsBeyondDatabaseColumnLengths() {
        RoomScheduleRequestDto invalid = request();
        invalid.setMeetingType("T".repeat(21));

        assertThatThrownBy(() -> roomScheduleService.createSchedule(ROOM_ID, USER_ID, invalid))
                .isInstanceOf(InvalidScheduleRequestException.class);
        verifyNoInteractions(scheduleRepository);
    }

    private Schedule schedule(String useYn) {
        return Schedule.builder()
                .scheduleId(SCHEDULE_ID)
                .roomId(ROOM_ID)
                .userId(USER_ID)
                .planYmd("202610")
                .planHm("123000")
                .planAmpm("PM")
                .storeId(8L)
                .meetingType("MEETING")
                .useYn(useYn)
                .build();
    }

    private RoomScheduleRequestDto request() {
        RoomScheduleRequestDto request = new RoomScheduleRequestDto();
        request.setPlanYmd("202610");
        request.setPlanHm("123000");
        request.setPlanAmpm("PM");
        request.setStoreId(8L);
        request.setMeetingType("MEETING");
        return request;
    }
}
