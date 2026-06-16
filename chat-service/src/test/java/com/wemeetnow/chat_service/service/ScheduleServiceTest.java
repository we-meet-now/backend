package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.domain.UserSchedule;
import com.wemeetnow.chat_service.domain.UserScheduleParticipant;
import com.wemeetnow.chat_service.domain.enums.UserScheduleStatus;
import com.wemeetnow.chat_service.dto.*;
import com.wemeetnow.chat_service.exception.InvalidScheduleTimeException;
import com.wemeetnow.chat_service.exception.ScheduleAccessDeniedException;
import com.wemeetnow.chat_service.exception.ScheduleNotFoundException;
import com.wemeetnow.chat_service.repository.UserScheduleParticipantRepository;
import com.wemeetnow.chat_service.repository.UserScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService 단위 테스트")
class ScheduleServiceTest {

    @Mock
    private UserScheduleRepository userScheduleRepository;

    @Mock
    private UserScheduleParticipantRepository userScheduleParticipantRepository;

    @Mock
    private RestClient.Builder restClientBuilder;

    @InjectMocks
    private ScheduleService scheduleService;

    // ──────────────────────────────────────────────────────────────
    // 공통 픽스처
    // ──────────────────────────────────────────────────────────────

    private static final Long CREATOR_USER_ID  = 1L;
    private static final Long OTHER_USER_ID    = 2L;
    private static final Long SCHEDULE_ID      = 10L;
    private static final Long PARTICIPANT_ID   = 100L; // 생성자의 UserScheduleParticipant PK

    private final LocalDateTime NOW   = LocalDateTime.now();
    private final LocalDateTime START = NOW.plusDays(1);
    private final LocalDateTime END   = NOW.plusDays(2);

    /** 기본 일정 엔티티 (공개) */
    private UserSchedule publicSchedule() {
        return UserSchedule.builder()
                .userScheduleId(SCHEDULE_ID)
                .title("테스트 일정")
                .startAt(START)
                .endAt(END)
                .status(UserScheduleStatus.PENDING)
                .publicScheduleYn("Y")
                .publicMemberYn("Y")
                .createdAt(NOW)
                .modifiedAt(NOW)
                .userScheduleParticipantId(PARTICIPANT_ID)
                .build();
    }

    /** 생성자의 참여자 엔티티 */
    private UserScheduleParticipant creatorParticipant() {
        return UserScheduleParticipant.builder()
                .userScheduleParticipantId(PARTICIPANT_ID)
                .userId(CREATOR_USER_ID)
                .userScheduleId(SCHEDULE_ID)
                .createdAt(NOW)
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    // 일정 생성
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createSchedule")
    class CreateScheduleTests {

        private CreateScheduleRequestDto buildRequest(LocalDateTime start, LocalDateTime end,
                                                      List<Long> participantIds) {
            CreateScheduleRequestDto dto = new CreateScheduleRequestDto();
            dto.setTitle("테스트 일정");
            dto.setStartAt(start);
            dto.setEndAt(end);
            dto.setPublicScheduleYn("Y");
            dto.setPublicMemberYn("Y");
            dto.setParticipantIds(participantIds);
            return dto;
        }

        @Test
        @DisplayName("정상 생성 - 참여자 없음")
        void createSchedule_noParticipants_success() {
            // given
            CreateScheduleRequestDto request = buildRequest(START, END, List.of());

            // save 호출 시 PK를 세팅하는 stub
            doAnswer(inv -> {
                UserScheduleParticipant p = inv.getArgument(0);
                ReflectionTestUtils.setField(p, "userScheduleParticipantId", PARTICIPANT_ID);
                return p;
            }).when(userScheduleParticipantRepository).save(any(UserScheduleParticipant.class));

            doAnswer(inv -> {
                UserSchedule s = inv.getArgument(0);
                ReflectionTestUtils.setField(s, "userScheduleId", SCHEDULE_ID);
                return s;
            }).when(userScheduleRepository).save(any(UserSchedule.class));

            // when
            Long scheduleId = scheduleService.createSchedule(CREATOR_USER_ID, request);

            // then
            assertThat(scheduleId).isEqualTo(SCHEDULE_ID);
            // 생성자 participant 저장 2번 (최초 + scheduleId 역방향 세팅), schedule 저장 1번
            verify(userScheduleParticipantRepository, times(2)).save(any(UserScheduleParticipant.class));
            verify(userScheduleRepository, times(1)).save(any(UserSchedule.class));
        }

        @Test
        @DisplayName("정상 생성 - 초대 참여자 2명 포함")
        void createSchedule_withParticipants_success() {
            // given
            List<Long> invited = List.of(2L, 3L);
            CreateScheduleRequestDto request = buildRequest(START, END, invited);

            doAnswer(inv -> {
                UserScheduleParticipant p = inv.getArgument(0);
                ReflectionTestUtils.setField(p, "userScheduleParticipantId", PARTICIPANT_ID);
                return p;
            }).when(userScheduleParticipantRepository).save(any(UserScheduleParticipant.class));

            doAnswer(inv -> {
                UserSchedule s = inv.getArgument(0);
                ReflectionTestUtils.setField(s, "userScheduleId", SCHEDULE_ID);
                return s;
            }).when(userScheduleRepository).save(any(UserSchedule.class));

            // when
            Long scheduleId = scheduleService.createSchedule(CREATOR_USER_ID, request);

            // then
            assertThat(scheduleId).isEqualTo(SCHEDULE_ID);
            // 생성자 2번 + 초대 참여자 2명 = 총 4번
            verify(userScheduleParticipantRepository, times(4)).save(any(UserScheduleParticipant.class));
        }

        @Test
        @DisplayName("시작 시간이 종료 시간보다 미래이면 InvalidScheduleTimeException")
        void createSchedule_invalidTime_throwsException() {
            // given: start > end
            CreateScheduleRequestDto request = buildRequest(END, START, List.of());

            // when & then
            assertThatThrownBy(() -> scheduleService.createSchedule(CREATOR_USER_ID, request))
                    .isInstanceOf(InvalidScheduleTimeException.class)
                    .hasMessageContaining("이전이어야");
        }

        @Test
        @DisplayName("참여자 목록에 생성자 ID가 포함되어도 중복 등록하지 않음")
        void createSchedule_creatorInParticipantList_notDuplicated() {
            // given: participantIds에 creatorUserId 포함
            CreateScheduleRequestDto request = buildRequest(START, END, List.of(CREATOR_USER_ID, 2L));

            doAnswer(inv -> {
                UserScheduleParticipant p = inv.getArgument(0);
                ReflectionTestUtils.setField(p, "userScheduleParticipantId", PARTICIPANT_ID);
                return p;
            }).when(userScheduleParticipantRepository).save(any(UserScheduleParticipant.class));

            doAnswer(inv -> {
                UserSchedule s = inv.getArgument(0);
                ReflectionTestUtils.setField(s, "userScheduleId", SCHEDULE_ID);
                return s;
            }).when(userScheduleRepository).save(any(UserSchedule.class));

            // when
            scheduleService.createSchedule(CREATOR_USER_ID, request);

            // then: 생성자(2번) + 2L 한 명(1번) = 총 3번 (creatorUserId는 skip)
            verify(userScheduleParticipantRepository, times(3)).save(any(UserScheduleParticipant.class));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 일정 상세 조회
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getScheduleDetail")
    class GetScheduleDetailTests {

        @Test
        @DisplayName("공개 일정 - 비참여자도 전체 정보 조회 성공")
        void getScheduleDetail_publicSchedule_success() {
            // given
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(publicSchedule()));
            given(userScheduleParticipantRepository.existsByUserScheduleIdAndUserId(SCHEDULE_ID, OTHER_USER_ID))
                    .willReturn(false);
            given(userScheduleParticipantRepository.findByUserScheduleId(SCHEDULE_ID))
                    .willReturn(List.of(creatorParticipant()));

            // when
            ScheduleDetailResponseDto result = scheduleService.getScheduleDetail(SCHEDULE_ID, OTHER_USER_ID);

            // then
            assertThat(result.getTitle()).isEqualTo("테스트 일정");
            assertThat(result.getParticipants()).hasSize(1);
        }

        @Test
        @DisplayName("비공개 일정(publicScheduleYn=N) - 비참여자 조회 시 내용 마스킹")
        void getScheduleDetail_privateSchedule_nonParticipant_masked() {
            // given
            UserSchedule privateSchedule = UserSchedule.builder()
                    .userScheduleId(SCHEDULE_ID)
                    .status(UserScheduleStatus.PENDING)
                    .publicScheduleYn("N")
                    .publicMemberYn("N")
                    .userScheduleParticipantId(PARTICIPANT_ID)
                    .build();

            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(privateSchedule));
            given(userScheduleParticipantRepository.existsByUserScheduleIdAndUserId(SCHEDULE_ID, OTHER_USER_ID))
                    .willReturn(false);

            // when
            ScheduleDetailResponseDto result = scheduleService.getScheduleDetail(SCHEDULE_ID, OTHER_USER_ID);

            // then: title, startAt 등은 null (마스킹)
            assertThat(result.getTitle()).isNull();
            assertThat(result.getStartAt()).isNull();
            assertThat(result.getParticipants()).isNull();
            assertThat(result.getUserScheduleId()).isEqualTo(SCHEDULE_ID);
        }

        @Test
        @DisplayName("참여자 비공개(publicMemberYn=N) - 비참여자는 participants null")
        void getScheduleDetail_privateMember_nonParticipant_participantsNull() {
            // given
            UserSchedule schedule = UserSchedule.builder()
                    .userScheduleId(SCHEDULE_ID)
                    .title("일정 제목")
                    .startAt(START).endAt(END)
                    .status(UserScheduleStatus.PENDING)
                    .publicScheduleYn("Y")
                    .publicMemberYn("N")
                    .userScheduleParticipantId(PARTICIPANT_ID)
                    .build();

            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
            given(userScheduleParticipantRepository.existsByUserScheduleIdAndUserId(SCHEDULE_ID, OTHER_USER_ID))
                    .willReturn(false);

            // when
            ScheduleDetailResponseDto result = scheduleService.getScheduleDetail(SCHEDULE_ID, OTHER_USER_ID);

            // then: 일정 정보는 공개, 참여자 목록은 null
            assertThat(result.getTitle()).isEqualTo("일정 제목");
            assertThat(result.getParticipants()).isNull();
        }

        @Test
        @DisplayName("참여자는 publicMemberYn=N이어도 참여자 목록 조회 가능")
        void getScheduleDetail_privateMember_participant_canSeeParticipants() {
            // given
            UserSchedule schedule = UserSchedule.builder()
                    .userScheduleId(SCHEDULE_ID)
                    .title("일정 제목")
                    .startAt(START).endAt(END)
                    .status(UserScheduleStatus.PENDING)
                    .publicScheduleYn("Y")
                    .publicMemberYn("N")
                    .userScheduleParticipantId(PARTICIPANT_ID)
                    .build();

            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
            given(userScheduleParticipantRepository.existsByUserScheduleIdAndUserId(SCHEDULE_ID, CREATOR_USER_ID))
                    .willReturn(true);
            given(userScheduleParticipantRepository.findByUserScheduleId(SCHEDULE_ID))
                    .willReturn(List.of(creatorParticipant()));

            // when
            ScheduleDetailResponseDto result = scheduleService.getScheduleDetail(SCHEDULE_ID, CREATOR_USER_ID);

            // then
            assertThat(result.getParticipants()).hasSize(1);
        }

        @Test
        @DisplayName("존재하지 않는 일정 조회 시 ScheduleNotFoundException")
        void getScheduleDetail_notFound_throwsException() {
            // given
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scheduleService.getScheduleDetail(SCHEDULE_ID, CREATOR_USER_ID))
                    .isInstanceOf(ScheduleNotFoundException.class)
                    .hasMessageContaining("존재하지 않는 일정");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 일정 수정
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateSchedule")
    class UpdateScheduleTests {

        private UpdateScheduleRequestDto buildUpdateRequest(LocalDateTime start, LocalDateTime end) {
            UpdateScheduleRequestDto dto = new UpdateScheduleRequestDto();
            dto.setTitle("수정된 제목");
            dto.setStartAt(start);
            dto.setEndAt(end);
            dto.setPublicScheduleYn("N");
            dto.setPublicMemberYn("N");
            return dto;
        }

        @Test
        @DisplayName("소유자가 수정 요청 시 성공")
        void updateSchedule_owner_success() {
            // given
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(publicSchedule()));
            given(userScheduleParticipantRepository.findById(PARTICIPANT_ID))
                    .willReturn(Optional.of(creatorParticipant()));
            given(userScheduleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            UpdateScheduleRequestDto request = buildUpdateRequest(START, END);

            // when
            UpdateScheduleResponseDto result = scheduleService.updateSchedule(SCHEDULE_ID, CREATOR_USER_ID, request);

            // then
            assertThat(result.getUserScheduleId()).isEqualTo(SCHEDULE_ID);
            verify(userScheduleRepository).save(argThat(s -> "수정된 제목".equals(s.getTitle())));
        }

        @Test
        @DisplayName("소유자가 아닌 사용자가 수정 요청 시 ScheduleAccessDeniedException")
        void updateSchedule_notOwner_throwsException() {
            // given
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(publicSchedule()));
            given(userScheduleParticipantRepository.findById(PARTICIPANT_ID))
                    .willReturn(Optional.of(creatorParticipant())); // owner = CREATOR_USER_ID

            UpdateScheduleRequestDto request = buildUpdateRequest(START, END);

            // when & then (OTHER_USER_ID로 수정 시도)
            assertThatThrownBy(() -> scheduleService.updateSchedule(SCHEDULE_ID, OTHER_USER_ID, request))
                    .isInstanceOf(ScheduleAccessDeniedException.class)
                    .hasMessageContaining("권한이 없습니다");
        }

        @Test
        @DisplayName("존재하지 않는 일정 수정 시 ScheduleNotFoundException")
        void updateSchedule_notFound_throwsException() {
            // given
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

            UpdateScheduleRequestDto request = buildUpdateRequest(START, END);

            // when & then
            assertThatThrownBy(() -> scheduleService.updateSchedule(SCHEDULE_ID, CREATOR_USER_ID, request))
                    .isInstanceOf(ScheduleNotFoundException.class);
        }

        @Test
        @DisplayName("수정 시 시간 역전이면 InvalidScheduleTimeException")
        void updateSchedule_invalidTime_throwsException() {
            // given
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(publicSchedule()));
            given(userScheduleParticipantRepository.findById(PARTICIPANT_ID))
                    .willReturn(Optional.of(creatorParticipant()));

            UpdateScheduleRequestDto request = buildUpdateRequest(END, START); // start > end

            // when & then
            assertThatThrownBy(() -> scheduleService.updateSchedule(SCHEDULE_ID, CREATOR_USER_ID, request))
                    .isInstanceOf(InvalidScheduleTimeException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 일정 상태 변경
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateScheduleStatus")
    class UpdateScheduleStatusTests {

        @Test
        @DisplayName("소유자가 상태 변경 요청 시 성공")
        void updateScheduleStatus_owner_success() {
            // given
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(publicSchedule()));
            given(userScheduleParticipantRepository.findById(PARTICIPANT_ID))
                    .willReturn(Optional.of(creatorParticipant()));
            given(userScheduleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            UpdateScheduleStatusResponseDto result = scheduleService
                    .updateScheduleStatus(SCHEDULE_ID, CREATOR_USER_ID, UserScheduleStatus.CONFIRMED);

            // then
            assertThat(result.getStatus()).isEqualTo(UserScheduleStatus.CONFIRMED);
            assertThat(result.getUserScheduleId()).isEqualTo(SCHEDULE_ID);
        }

        @Test
        @DisplayName("소유자가 아닌 사용자가 상태 변경 시 ScheduleAccessDeniedException")
        void updateScheduleStatus_notOwner_throwsException() {
            // given
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(publicSchedule()));
            given(userScheduleParticipantRepository.findById(PARTICIPANT_ID))
                    .willReturn(Optional.of(creatorParticipant()));

            // when & then
            assertThatThrownBy(() -> scheduleService
                    .updateScheduleStatus(SCHEDULE_ID, OTHER_USER_ID, UserScheduleStatus.CANCELLED))
                    .isInstanceOf(ScheduleAccessDeniedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 일정 상태 변경 시 ScheduleNotFoundException")
        void updateScheduleStatus_notFound_throwsException() {
            // given
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scheduleService
                    .updateScheduleStatus(SCHEDULE_ID, CREATOR_USER_ID, UserScheduleStatus.CONFIRMED))
                    .isInstanceOf(ScheduleNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 일정 삭제
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteSchedule")
    class DeleteScheduleTests {

        @Test
        @DisplayName("소유자가 삭제 요청 시 일정 및 참여자 모두 삭제")
        void deleteSchedule_owner_success() {
            // given
            UserSchedule schedule = publicSchedule();
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
            given(userScheduleParticipantRepository.findById(PARTICIPANT_ID))
                    .willReturn(Optional.of(creatorParticipant()));

            // when
            scheduleService.deleteSchedule(SCHEDULE_ID, CREATOR_USER_ID);

            // then
            verify(userScheduleParticipantRepository).deleteByUserScheduleId(SCHEDULE_ID);
            verify(userScheduleRepository).delete(schedule);
        }

        @Test
        @DisplayName("소유자가 아닌 사용자가 삭제 요청 시 ScheduleAccessDeniedException")
        void deleteSchedule_notOwner_throwsException() {
            // given
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(publicSchedule()));
            given(userScheduleParticipantRepository.findById(PARTICIPANT_ID))
                    .willReturn(Optional.of(creatorParticipant()));

            // when & then
            assertThatThrownBy(() -> scheduleService.deleteSchedule(SCHEDULE_ID, OTHER_USER_ID))
                    .isInstanceOf(ScheduleAccessDeniedException.class);

            verify(userScheduleRepository, never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 일정 삭제 시 ScheduleNotFoundException")
        void deleteSchedule_notFound_throwsException() {
            // given
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scheduleService.deleteSchedule(SCHEDULE_ID, CREATOR_USER_ID))
                    .isInstanceOf(ScheduleNotFoundException.class);
        }
    }
}

