package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.client.AuthServiceClient;
import com.wemeetnow.chat_service.domain.UserSchedule;
import com.wemeetnow.chat_service.domain.UserScheduleParticipant;
import com.wemeetnow.chat_service.domain.enums.UserScheduleStatus;
import com.wemeetnow.chat_service.dto.*;
import com.wemeetnow.chat_service.exception.InvalidScheduleRequestException;
import com.wemeetnow.chat_service.exception.InvalidScheduleMonthException;
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

import java.time.LocalDateTime;
import java.time.YearMonth;
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
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private ScheduleService scheduleService;

    // ──────────────────────────────────────────────────────────────
    // 공통 픽스처
    // ──────────────────────────────────────────────────────────────

    private static final Long CREATOR_USER_ID  = 1L;
    private static final Long OTHER_USER_ID    = 2L;
    private static final Long SCHEDULE_ID      = 10L;
    private static final Long PARTICIPANT_ID   = 100L; // 생성자의 UserScheduleParticipant PK
    private static final String START_DT = "20261017";
    private static final String START_TIM = "100000";
    private static final String END_DT = "20261018";
    private static final String END_TIM = "110000";

    private final LocalDateTime NOW   = LocalDateTime.now();
    private final LocalDateTime START = NOW.plusDays(1);
    private final LocalDateTime END   = NOW.plusDays(2);

    /** 기본 일정 엔티티 (공개) */
    private UserSchedule publicSchedule() {
        return UserSchedule.builder()
                .userScheduleId(SCHEDULE_ID)
                .title("테스트 일정")
                .startDt(START_DT)
                .startTim(START_TIM)
                .endDt(END_DT)
                .endTim(END_TIM)
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

    private CommonApiResponse<List<AuthFriendInfoDto>> friendResponse(Long friendId, String status) {
        AuthFriendInfoDto friend = new AuthFriendInfoDto();
        friend.setFriendId(friendId);
        friend.setFriendStatus(status);
        return CommonApiResponse.<List<AuthFriendInfoDto>>builder()
                .data(friendId == null ? List.of() : List.of(friend))
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    // 일정 생성
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createSchedule")
    class CreateScheduleTests {

        private CreateScheduleRequestDto buildRequest(
                String startDt, String startTim, String endDt, String endTim, List<Long> participantIds) {
            CreateScheduleRequestDto dto = new CreateScheduleRequestDto();
            dto.setTitle("테스트 일정");
            dto.setStartDt(startDt);
            dto.setStartTim(startTim);
            dto.setEndDt(endDt);
            dto.setEndTim(endTim);
            dto.setPublicScheduleYn("Y");
            dto.setPublicMemberYn("Y");
            dto.setParticipantIds(participantIds);
            return dto;
        }

        @Test
        @DisplayName("정상 생성 - 참여자 없음")
        void createSchedule_noParticipants_success() {
            // given
            CreateScheduleRequestDto request = buildRequest(START_DT, START_TIM, END_DT, END_TIM, null);

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
            CreateScheduleRequestDto request = buildRequest(START_DT, START_TIM, END_DT, END_TIM, invited);

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
            CreateScheduleRequestDto request = buildRequest(END_DT, END_TIM, START_DT, START_TIM, List.of());

            // when & then
            assertThatThrownBy(() -> scheduleService.createSchedule(CREATOR_USER_ID, request))
                    .isInstanceOf(InvalidScheduleTimeException.class)
                    .hasMessageContaining("이전이어야");
        }

        @Test
        @DisplayName("형식이 잘못된 날짜와 누락된 시간은 한글 오류로 거부")
        void createSchedule_invalidDateOrMissingTime_rejected() {
            CreateScheduleRequestDto invalidDate =
                    buildRequest("20260230", START_TIM, END_DT, END_TIM, null);
            CreateScheduleRequestDto missingTime =
                    buildRequest(START_DT, null, END_DT, END_TIM, null);

            assertThatThrownBy(() -> scheduleService.createSchedule(CREATOR_USER_ID, invalidDate))
                    .isInstanceOf(InvalidScheduleTimeException.class)
                    .hasMessageContaining("유효하지 않습니다");
            assertThatThrownBy(() -> scheduleService.createSchedule(CREATOR_USER_ID, missingTime))
                    .isInstanceOf(InvalidScheduleTimeException.class)
                    .hasMessageContaining("startTim")
                    .hasMessageContaining("HHmmss");
            verifyNoInteractions(userScheduleParticipantRepository, userScheduleRepository);
        }

        @Test
        @DisplayName("참여자 목록에 생성자 ID가 포함되어도 중복 등록하지 않음")
        void createSchedule_creatorInParticipantList_notDuplicated() {
            // given: participantIds에 creatorUserId 포함
            CreateScheduleRequestDto request = buildRequest(
                    START_DT, START_TIM, END_DT, END_TIM, List.of(CREATOR_USER_ID, 2L));

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

        @Test
        @DisplayName("공개 설정을 생략하면 비공개로 저장")
        void createSchedule_visibilityOmitted_defaultsToPrivate() {
            CreateScheduleRequestDto request = buildRequest(START_DT, START_TIM, END_DT, END_TIM, List.of());
            request.setPublicScheduleYn(null);
            request.setPublicMemberYn(null);
            doAnswer(inv -> {
                UserScheduleParticipant participant = inv.getArgument(0);
                ReflectionTestUtils.setField(participant, "userScheduleParticipantId", PARTICIPANT_ID);
                return participant;
            }).when(userScheduleParticipantRepository).save(any(UserScheduleParticipant.class));
            doAnswer(inv -> {
                UserSchedule schedule = inv.getArgument(0);
                ReflectionTestUtils.setField(schedule, "userScheduleId", SCHEDULE_ID);
                return schedule;
            }).when(userScheduleRepository).save(any(UserSchedule.class));

            scheduleService.createSchedule(CREATOR_USER_ID, request);

            verify(userScheduleRepository).save(argThat(saved ->
                    "N".equals(saved.getPublicScheduleYn()) && "N".equals(saved.getPublicMemberYn())));
        }

        @Test
        @DisplayName("잘못된 공개 설정을 거부")
        void createSchedule_invalidVisibility_rejected() {
            CreateScheduleRequestDto request = buildRequest(START_DT, START_TIM, END_DT, END_TIM, List.of());
            request.setPublicScheduleYn("PUBLIC");

            assertThatThrownBy(() -> scheduleService.createSchedule(CREATOR_USER_ID, request))
                    .isInstanceOf(InvalidScheduleRequestException.class)
                    .hasMessageContaining("Y 또는 N");
            verifyNoInteractions(userScheduleParticipantRepository, userScheduleRepository);
        }

        @Test
        @DisplayName("참여자 ID 0은 한글 오류로 거부")
        void createSchedule_nonPositiveParticipantId_rejected() {
            CreateScheduleRequestDto request = buildRequest(
                    START_DT, START_TIM, END_DT, END_TIM, List.of(0L));

            assertThatThrownBy(() -> scheduleService.createSchedule(CREATOR_USER_ID, request))
                    .isInstanceOf(InvalidScheduleRequestException.class)
                    .hasMessageContaining("참여자 ID")
                    .hasMessageContaining("1 이상의 숫자");
            verifyNoInteractions(userScheduleParticipantRepository, userScheduleRepository);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 일정 목록 조회
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSchedules")
    class GetSchedulesTests {

        @Test
        @DisplayName("선택한 월에 생성한 일정만 시작일 오름차순으로 조회하고 yyyyMMdd로 반환")
        void getSchedules_monthProvided_sortsAndFormatsDates() {
            String monthStart = "20260701";
            String nextMonthStart = "20260801";
            UserSchedule laterSchedule = UserSchedule.builder()
                    .userScheduleId(11L)
                    .title("늦은 일정")
                    .startDt("20260720")
                    .startTim("140000")
                    .endDt("20260720")
                    .endTim("160000")
                    .status(UserScheduleStatus.PENDING)
                    .publicScheduleYn("Y")
                    .publicMemberYn("Y")
                    .build();
            UserSchedule earlierSchedule = UserSchedule.builder()
                    .userScheduleId(12L)
                    .title("이른 일정")
                    .startDt("20260703")
                    .startTim("090000")
                    .endDt("20260703")
                    .endTim("100000")
                    .status(UserScheduleStatus.CONFIRMED)
                    .publicScheduleYn("N")
                    .publicMemberYn("N")
                    .build();

            given(userScheduleRepository.findCreatedBetween(CREATOR_USER_ID, monthStart, nextMonthStart))
                    .willReturn(List.of(laterSchedule, earlierSchedule));

            List<ScheduleListItemResponseDto> result = scheduleService.getSchedules(CREATOR_USER_ID, "202607");

            assertThat(result).extracting(ScheduleListItemResponseDto::getUserScheduleId)
                    .containsExactly(12L, 11L);
            assertThat(result).extracting(ScheduleListItemResponseDto::getStartDt)
                    .containsExactly("20260703", "20260720");
            assertThat(result).extracting(ScheduleListItemResponseDto::getStartTim)
                    .containsExactly("090000", "140000");
            assertThat(result.get(0).getEndDt()).isEqualTo("20260703");
            assertThat(result.get(0).getEndTim()).isEqualTo("100000");
            verify(userScheduleRepository).findCreatedBetween(CREATOR_USER_ID, monthStart, nextMonthStart);
        }

        @Test
        @DisplayName("월을 생략하면 현재 월의 시작과 다음 달 시작으로 조회")
        void getSchedules_monthOmitted_usesCurrentMonth() {
            YearMonth currentMonth = YearMonth.now();
            String monthStart = currentMonth.atDay(1).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            String nextMonthStart = currentMonth.plusMonths(1).atDay(1)
                    .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            given(userScheduleRepository.findCreatedBetween(CREATOR_USER_ID, monthStart, nextMonthStart))
                    .willReturn(List.of());

            List<ScheduleListItemResponseDto> result = scheduleService.getSchedules(CREATOR_USER_ID, null);

            assertThat(result).isEmpty();
            verify(userScheduleRepository).findCreatedBetween(CREATOR_USER_ID, monthStart, nextMonthStart);
        }

        @Test
        @DisplayName("유효하지 않은 월 입력 시 InvalidScheduleMonthException")
        void getSchedules_invalidMonth_throwsException() {
            assertThatThrownBy(() -> scheduleService.getSchedules(CREATOR_USER_ID, "202613"))
                    .isInstanceOf(InvalidScheduleMonthException.class)
                    .hasMessageContaining("유효하지 않은 월");
            verifyNoInteractions(userScheduleRepository);
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
            given(userScheduleParticipantRepository.findById(PARTICIPANT_ID))
                    .willReturn(Optional.of(creatorParticipant()));
            given(authServiceClient.getFriendList("Bearer accepted"))
                    .willReturn(friendResponse(CREATOR_USER_ID, "NEW"));
            given(userScheduleParticipantRepository.findByUserScheduleId(SCHEDULE_ID))
                    .willReturn(List.of(creatorParticipant()));

            // when
            ScheduleDetailResponseDto result = scheduleService.getScheduleDetail(
                    SCHEDULE_ID, OTHER_USER_ID, "Bearer accepted");

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
            ScheduleDetailResponseDto result = scheduleService.getScheduleDetail(
                    SCHEDULE_ID, OTHER_USER_ID, "Bearer stranger");

            // then: title, startDt 등은 null (마스킹)
            assertThat(result.getTitle()).isNull();
            assertThat(result.getStartDt()).isNull();
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
                    .startDt(START_DT).startTim(START_TIM).endDt(END_DT).endTim(END_TIM)
                    .status(UserScheduleStatus.PENDING)
                    .publicScheduleYn("Y")
                    .publicMemberYn("N")
                    .userScheduleParticipantId(PARTICIPANT_ID)
                    .build();

            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
            given(userScheduleParticipantRepository.existsByUserScheduleIdAndUserId(SCHEDULE_ID, OTHER_USER_ID))
                    .willReturn(false);
            given(userScheduleParticipantRepository.findById(PARTICIPANT_ID))
                    .willReturn(Optional.of(creatorParticipant()));
            given(authServiceClient.getFriendList("Bearer stranger"))
                    .willReturn(friendResponse(CREATOR_USER_ID, "FAVORITE"));

            // when
            ScheduleDetailResponseDto result = scheduleService.getScheduleDetail(
                    SCHEDULE_ID, OTHER_USER_ID, "Bearer stranger");

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
                    .startDt(START_DT).startTim(START_TIM).endDt(END_DT).endTim(END_TIM)
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
            ScheduleDetailResponseDto result = scheduleService.getScheduleDetail(
                    SCHEDULE_ID, CREATOR_USER_ID, "Bearer owner");

            // then
            assertThat(result.getParticipants()).hasSize(1);
        }

        @Test
        @DisplayName("존재하지 않는 일정 조회 시 ScheduleNotFoundException")
        void getScheduleDetail_notFound_throwsException() {
            // given
            given(userScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scheduleService.getScheduleDetail(
                    SCHEDULE_ID, CREATOR_USER_ID, "Bearer owner"))
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

        private UpdateScheduleRequestDto buildUpdateRequest(
                String startDt, String startTim, String endDt, String endTim) {
            UpdateScheduleRequestDto dto = new UpdateScheduleRequestDto();
            dto.setTitle("수정된 제목");
            dto.setStartDt(startDt);
            dto.setStartTim(startTim);
            dto.setEndDt(endDt);
            dto.setEndTim(endTim);
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

            UpdateScheduleRequestDto request = buildUpdateRequest(START_DT, START_TIM, END_DT, END_TIM);

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

            UpdateScheduleRequestDto request = buildUpdateRequest(START_DT, START_TIM, END_DT, END_TIM);

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

            UpdateScheduleRequestDto request = buildUpdateRequest(START_DT, START_TIM, END_DT, END_TIM);

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

            UpdateScheduleRequestDto request =
                    buildUpdateRequest(END_DT, END_TIM, START_DT, START_TIM); // start > end

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
