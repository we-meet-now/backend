package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.dto.AuthUserDto;
import com.wemeetnow.chat_service.dto.RoomScheduleResponseDto;
import com.wemeetnow.chat_service.exception.GlobalExceptionHandler;
import com.wemeetnow.chat_service.exception.ScheduleAccessDeniedException;
import com.wemeetnow.chat_service.service.RoomScheduleService;
import com.wemeetnow.chat_service.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoomScheduleControllerTest {

    private static final Long ROOM_ID = 12L;
    private static final Long SCHEDULE_ID = 56L;
    private static final Long USER_ID = 34L;
    private static final String BASE_URL = "/api/chat/v1/chat-rooms/" + ROOM_ID + "/schedules";

    @Mock
    private RoomScheduleService roomScheduleService;

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private RoomScheduleController controller;

    private MockMvc mockMvc;
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        AuthUserDto user = new AuthUserDto();
        user.setUserId(USER_ID);
        given(scheduleService.fetchUserFromAuthService(anyString())).willReturn(user);
    }

    @Test
    void listsRoomSchedules() throws Exception {
        given(roomScheduleService.getSchedules(ROOM_ID, USER_ID)).willReturn(List.of(scheduleDto()));

        mockMvc.perform(get(BASE_URL).header("Authorization", "Bearer sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("2000"))
                .andExpect(jsonPath("$.data[0].scheduleId").value(SCHEDULE_ID));
    }

    @Test
    void createsRoomSchedule() throws Exception {
        given(roomScheduleService.createSchedule(eq(ROOM_ID), eq(USER_ID), any()))
                .willReturn(scheduleDto());

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer sample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planYmd":"202610","planHm":"123000","planAmpm":"PM","storeId":8,"meetingType":"MEETING"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("2001"))
                .andExpect(jsonPath("$.data.userId").value(USER_ID));
    }

    @Test
    void readsAndUpdatesSpecificRoomSchedule() throws Exception {
        given(roomScheduleService.getSchedule(ROOM_ID, SCHEDULE_ID, USER_ID)).willReturn(scheduleDto());
        given(roomScheduleService.updateSchedule(eq(ROOM_ID), eq(SCHEDULE_ID), eq(USER_ID), any()))
                .willReturn(scheduleDto());

        mockMvc.perform(get(BASE_URL + "/" + SCHEDULE_ID).header("Authorization", "Bearer sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomId").value(ROOM_ID));
        mockMvc.perform(put(BASE_URL + "/" + SCHEDULE_ID)
                        .header("Authorization", "Bearer sample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planYmd":"202611","planHm":"140000","planAmpm":"PM","meetingType":"DINNER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("2002"));
    }

    @Test
    void softDeletesRoomSchedule() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + SCHEDULE_ID).header("Authorization", "Bearer sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("2003"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void returnsForbiddenForNonMember() throws Exception {
        given(roomScheduleService.getSchedules(ROOM_ID, USER_ID))
                .willThrow(new ScheduleAccessDeniedException("not a member"));

        mockMvc.perform(get(BASE_URL).header("Authorization", "Bearer sample"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value("4003"));
    }

    private RoomScheduleResponseDto scheduleDto() {
        return RoomScheduleResponseDto.builder()
                .scheduleId(SCHEDULE_ID)
                .roomId(ROOM_ID)
                .userId(USER_ID)
                .planYmd("202610")
                .planHm("123000")
                .planAmpm("PM")
                .storeId(8L)
                .meetingType("MEETING")
                .useYn("Y")
                .build();
    }
}
