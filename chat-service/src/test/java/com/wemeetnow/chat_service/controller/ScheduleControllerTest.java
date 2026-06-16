package com.wemeetnow.chat_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wemeetnow.chat_service.domain.enums.UserScheduleStatus;
import com.wemeetnow.chat_service.dto.*;
import com.wemeetnow.chat_service.exception.GlobalExceptionHandler;
import com.wemeetnow.chat_service.exception.InvalidScheduleTimeException;
import com.wemeetnow.chat_service.exception.ScheduleAccessDeniedException;
import com.wemeetnow.chat_service.exception.ScheduleNotFoundException;
import com.wemeetnow.chat_service.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleController MockMvc 테스트")
class ScheduleControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private ScheduleController scheduleController;

    private static final String BASE_URL = "/api/v1/schedules";
    private static final String AUTH_TOKEN = "Bearer test-jwt-token";
    private static final Long SCHEDULE_ID = 10L;
    private static final Long USER_ID = 1L;

    private final LocalDateTime START = LocalDateTime.of(2026, 7, 1, 10, 0);
    private final LocalDateTime END   = LocalDateTime.of(2026, 7, 1, 12, 0);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime 직렬화 지원

        mockMvc = MockMvcBuilders
                .standaloneSetup(scheduleController)
                .setControllerAdvice(new GlobalExceptionHandler()) // 예외 핸들러 등록
                .build();

        // 모든 테스트에서 공통으로 사용: fetchUserFromAuthService mock
        AuthUserDto authUserDto = new AuthUserDto();
        authUserDto.setUserId(USER_ID);
        given(scheduleService.fetchUserFromAuthService(anyString())).willReturn(authUserDto);
    }

    // ──────────────────────────────────────────────────────────────
    // POST /api/v1/schedules  →  일정 생성
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/schedules - 일정 생성")
    class CreateScheduleControllerTests {

        @Test
        @DisplayName("정상 생성 시 201 CREATED 및 statusCode=2001 반환")
        void createSchedule_success_returns201() throws Exception {
            // given
            CreateScheduleRequestDto request = new CreateScheduleRequestDto();
            request.setTitle("팀 미팅");
            request.setStartAt(START);
            request.setEndAt(END);
            request.setPublicScheduleYn("Y");
            request.setPublicMemberYn("Y");
            request.setParticipantIds(List.of(2L, 3L));

            given(scheduleService.createSchedule(eq(USER_ID), any(CreateScheduleRequestDto.class)))
                    .willReturn(SCHEDULE_ID);

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", AUTH_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.statusCode").value("2001"))
                    .andExpect(jsonPath("$.message").value("일정 생성 성공"))
                    .andExpect(jsonPath("$.data.userScheduleId").value(SCHEDULE_ID));
        }

        @Test
        @DisplayName("시간 역전 입력 시 400 Bad Request 및 statusCode=4000 반환")
        void createSchedule_invalidTime_returns400() throws Exception {
            // given
            CreateScheduleRequestDto request = new CreateScheduleRequestDto();
            request.setTitle("잘못된 일정");
            request.setStartAt(END);   // start > end (역전)
            request.setEndAt(START);
            request.setPublicScheduleYn("Y");
            request.setPublicMemberYn("Y");
            request.setParticipantIds(List.of());

            given(scheduleService.createSchedule(eq(USER_ID), any(CreateScheduleRequestDto.class)))
                    .willThrow(new InvalidScheduleTimeException("일정 시작 시간은 종료 시간보다 이전이어야 합니다."));

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", AUTH_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value("4000"));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // GET /api/v1/schedules/scheduleId={id}  →  일정 상세 조회
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/schedules/scheduleId={id} - 일정 상세 조회")
    class GetScheduleDetailControllerTests {

        @Test
        @DisplayName("정상 조회 시 200 OK 및 statusCode=2000 반환")
        void getScheduleDetail_success_returns200() throws Exception {
            // given
            ScheduleDetailResponseDto response = ScheduleDetailResponseDto.builder()
                    .userScheduleId(SCHEDULE_ID)
                    .title("팀 미팅")
                    .startAt(START)
                    .endAt(END)
                    .status(UserScheduleStatus.PENDING)
                    .publicScheduleYn("Y")
                    .publicMemberYn("Y")
                    .participants(List.of(new ScheduleParticipantDto(100L, USER_ID, START)))
                    .build();

            given(scheduleService.getScheduleDetail(eq(SCHEDULE_ID), eq(USER_ID))).willReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/scheduleId=" + SCHEDULE_ID)
                            .header("Authorization", AUTH_TOKEN))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value("2000"))
                    .andExpect(jsonPath("$.message").value("일정 상세 조회 성공"))
                    .andExpect(jsonPath("$.data.userScheduleId").value(SCHEDULE_ID))
                    .andExpect(jsonPath("$.data.title").value("팀 미팅"))
                    .andExpect(jsonPath("$.data.participants").isArray());
        }

        @Test
        @DisplayName("존재하지 않는 일정 조회 시 404 Not Found 반환")
        void getScheduleDetail_notFound_returns404() throws Exception {
            // given
            given(scheduleService.getScheduleDetail(eq(SCHEDULE_ID), eq(USER_ID)))
                    .willThrow(new ScheduleNotFoundException("존재하지 않는 일정입니다. scheduleId=" + SCHEDULE_ID));

            // when & then
            mockMvc.perform(get(BASE_URL + "/scheduleId=" + SCHEDULE_ID)
                            .header("Authorization", AUTH_TOKEN))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value("4004"));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // PUT /api/v1/schedules/scheduleId={id}  →  일정 수정
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/schedules/scheduleId={id} - 일정 수정")
    class UpdateScheduleControllerTests {

        @Test
        @DisplayName("소유자 수정 요청 시 200 OK 및 statusCode=2002 반환")
        void updateSchedule_success_returns200() throws Exception {
            // given
            UpdateScheduleRequestDto request = new UpdateScheduleRequestDto();
            request.setTitle("수정된 제목");
            request.setStartAt(START);
            request.setEndAt(END);
            request.setPublicScheduleYn("N");
            request.setPublicMemberYn("N");

            given(scheduleService.updateSchedule(eq(SCHEDULE_ID), eq(USER_ID), any(UpdateScheduleRequestDto.class)))
                    .willReturn(new UpdateScheduleResponseDto(SCHEDULE_ID));

            // when & then
            mockMvc.perform(put(BASE_URL + "/scheduleId=" + SCHEDULE_ID)
                            .header("Authorization", AUTH_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value("2002"))
                    .andExpect(jsonPath("$.message").value("일정 수정 성공"))
                    .andExpect(jsonPath("$.data.userScheduleId").value(SCHEDULE_ID));
        }

        @Test
        @DisplayName("권한 없는 수정 요청 시 403 Forbidden 반환")
        void updateSchedule_notOwner_returns403() throws Exception {
            // given
            UpdateScheduleRequestDto request = new UpdateScheduleRequestDto();
            request.setTitle("수정된 제목");
            request.setStartAt(START);
            request.setEndAt(END);
            request.setPublicScheduleYn("Y");
            request.setPublicMemberYn("Y");

            given(scheduleService.updateSchedule(eq(SCHEDULE_ID), eq(USER_ID), any(UpdateScheduleRequestDto.class)))
                    .willThrow(new ScheduleAccessDeniedException("해당 일정을 수정/삭제할 권한이 없습니다."));

            // when & then
            mockMvc.perform(put(BASE_URL + "/scheduleId=" + SCHEDULE_ID)
                            .header("Authorization", AUTH_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value("4003"));
        }

        @Test
        @DisplayName("존재하지 않는 일정 수정 시 404 Not Found 반환")
        void updateSchedule_notFound_returns404() throws Exception {
            // given
            UpdateScheduleRequestDto request = new UpdateScheduleRequestDto();
            request.setTitle("수정된 제목");
            request.setStartAt(START);
            request.setEndAt(END);

            given(scheduleService.updateSchedule(eq(SCHEDULE_ID), eq(USER_ID), any(UpdateScheduleRequestDto.class)))
                    .willThrow(new ScheduleNotFoundException("존재하지 않는 일정입니다."));

            // when & then
            mockMvc.perform(put(BASE_URL + "/scheduleId=" + SCHEDULE_ID)
                            .header("Authorization", AUTH_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value("4004"));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // PATCH /api/v1/schedules/scheduleId={id}/status  →  상태 변경
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/schedules/scheduleId={id}/status - 상태 변경")
    class UpdateScheduleStatusControllerTests {

        @Test
        @DisplayName("상태 변경 성공 시 200 OK 및 statusCode=2002 반환")
        void updateScheduleStatus_success_returns200() throws Exception {
            // given
            UpdateStatusRequestDto request = new UpdateStatusRequestDto();
            request.setStatus(UserScheduleStatus.CONFIRMED);

            given(scheduleService.updateScheduleStatus(eq(SCHEDULE_ID), eq(USER_ID), eq(UserScheduleStatus.CONFIRMED)))
                    .willReturn(new UpdateScheduleStatusResponseDto(SCHEDULE_ID, UserScheduleStatus.CONFIRMED));

            // when & then
            mockMvc.perform(patch(BASE_URL + "/scheduleId=" + SCHEDULE_ID + "/status")
                            .header("Authorization", AUTH_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value("2002"))
                    .andExpect(jsonPath("$.message").value("일정 상태 변경 성공"))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("권한 없는 상태 변경 시 403 Forbidden 반환")
        void updateScheduleStatus_notOwner_returns403() throws Exception {
            // given
            UpdateStatusRequestDto request = new UpdateStatusRequestDto();
            request.setStatus(UserScheduleStatus.CANCELLED);

            given(scheduleService.updateScheduleStatus(eq(SCHEDULE_ID), eq(USER_ID), eq(UserScheduleStatus.CANCELLED)))
                    .willThrow(new ScheduleAccessDeniedException("해당 일정을 수정/삭제할 권한이 없습니다."));

            // when & then
            mockMvc.perform(patch(BASE_URL + "/scheduleId=" + SCHEDULE_ID + "/status")
                            .header("Authorization", AUTH_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value("4003"));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // DELETE /api/v1/schedules/scheduleId={id}  →  일정 삭제
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/schedules/scheduleId={id} - 일정 삭제")
    class DeleteScheduleControllerTests {

        @Test
        @DisplayName("소유자 삭제 요청 시 200 OK 및 statusCode=2003 반환")
        void deleteSchedule_success_returns200() throws Exception {
            // given
            doNothing().when(scheduleService).deleteSchedule(eq(SCHEDULE_ID), eq(USER_ID));

            // when & then
            mockMvc.perform(delete(BASE_URL + "/scheduleId=" + SCHEDULE_ID)
                            .header("Authorization", AUTH_TOKEN))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value("2003"))
                    .andExpect(jsonPath("$.message").value("일정 삭제 성공"));
        }

        @Test
        @DisplayName("권한 없는 삭제 요청 시 403 Forbidden 반환")
        void deleteSchedule_notOwner_returns403() throws Exception {
            // given
            doThrow(new ScheduleAccessDeniedException("해당 일정을 수정/삭제할 권한이 없습니다."))
                    .when(scheduleService).deleteSchedule(eq(SCHEDULE_ID), eq(USER_ID));

            // when & then
            mockMvc.perform(delete(BASE_URL + "/scheduleId=" + SCHEDULE_ID)
                            .header("Authorization", AUTH_TOKEN))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value("4003"));
        }

        @Test
        @DisplayName("존재하지 않는 일정 삭제 시 404 Not Found 반환")
        void deleteSchedule_notFound_returns404() throws Exception {
            // given
            doThrow(new ScheduleNotFoundException("존재하지 않는 일정입니다."))
                    .when(scheduleService).deleteSchedule(eq(SCHEDULE_ID), eq(USER_ID));

            // when & then
            mockMvc.perform(delete(BASE_URL + "/scheduleId=" + SCHEDULE_ID)
                            .header("Authorization", AUTH_TOKEN))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value("4004"));
        }
    }
}

