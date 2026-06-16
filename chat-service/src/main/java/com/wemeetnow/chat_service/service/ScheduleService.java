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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final UserScheduleRepository userScheduleRepository;
    private final UserScheduleParticipantRepository userScheduleParticipantRepository;
    private final RestClient.Builder restClientBuilder;

    @Value("${external.auth-service.url}")
    private String AUTH_SERVICE_URL;

    // ─────────────────────────────────────────────────────────
    // Auth 서비스 연동
    // ─────────────────────────────────────────────────────────

    /**
     * Auth 서비스로부터 토큰 기반 유저 정보 조회
     */
    public AuthUserDto fetchUserFromAuthService(String token) {
        try {
            String jwtHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
            RestClient restClient = restClientBuilder.baseUrl(AUTH_SERVICE_URL).build();
            return restClient.get()
                    .uri("/api/v1/users/get-id")
                    .header("Authorization", jwtHeader)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(AuthUserDto.class);
        } catch (Exception e) {
            log.error("Auth service 호출 실패: {}", e.getMessage());
            throw new RuntimeException("사용자 인증에 실패했습니다.");
        }
    }

    // ─────────────────────────────────────────────────────────
    // 일정 생성
    // ─────────────────────────────────────────────────────────

    /**
     * 개인 일정 생성 및 참여자 등록
     *
     * @param creatorUserId 생성자 userId (JWT에서 추출)
     * @param requestDto    일정 정보 + 초대할 참여자 ID 목록
     * @return 생성된 userScheduleId
     */
    @Transactional
    public Long createSchedule(Long creatorUserId, CreateScheduleRequestDto requestDto) {
        validateScheduleTime(requestDto.getStartAt(), requestDto.getEndAt());

        // 1) 생성자 UserScheduleParticipant 먼저 저장 (userScheduleId는 아직 없음)
        UserScheduleParticipant creatorParticipant = UserScheduleParticipant.builder()
                .userId(creatorUserId)
                .createdAt(LocalDateTime.now())
                .build();
        userScheduleParticipantRepository.save(creatorParticipant);

        // 2) UserSchedule 저장
        UserSchedule schedule = UserSchedule.builder()
                .title(requestDto.getTitle())
                .startAt(requestDto.getStartAt())
                .endAt(requestDto.getEndAt())
                .status(UserScheduleStatus.PENDING)
                .publicScheduleYn(requestDto.getPublicScheduleYn())
                .publicMemberYn(requestDto.getPublicMemberYn())
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .userScheduleParticipantId(creatorParticipant.getUserScheduleParticipantId())
                .build();
        userScheduleRepository.save(schedule);

        // 3) 생성자 참여자 레코드에 scheduleId 역방향 세팅
        creatorParticipant.setUserScheduleId(schedule.getUserScheduleId());
        userScheduleParticipantRepository.save(creatorParticipant);

        // 4) 초대된 참여자 등록
        if (requestDto.getParticipantIds() != null) {
            for (Long participantUserId : requestDto.getParticipantIds()) {
                if (participantUserId.equals(creatorUserId)) continue; // 중복 방지
                UserScheduleParticipant invitedParticipant = UserScheduleParticipant.builder()
                        .userId(participantUserId)
                        .userScheduleId(schedule.getUserScheduleId())
                        .createdAt(LocalDateTime.now())
                        .build();
                userScheduleParticipantRepository.save(invitedParticipant);
            }
        }

        return schedule.getUserScheduleId();
    }

    // ─────────────────────────────────────────────────────────
    // 일정 상세 조회
    // ─────────────────────────────────────────────────────────

    /**
     * 일정 상세 조회 (공개 여부 기반 마스킹 처리)
     *
     * @param scheduleId    조회할 일정 ID
     * @param loginedUserId 로그인한 사용자 ID
     */
    public ScheduleDetailResponseDto getScheduleDetail(Long scheduleId, Long loginedUserId) {
        UserSchedule schedule = findScheduleOrThrow(scheduleId);

        boolean isParticipant = userScheduleParticipantRepository
                .existsByUserScheduleIdAndUserId(scheduleId, loginedUserId);

        // publicScheduleYn = 'N' 이고 비참여자인 경우 → 일정 내용 마스킹
        if ("N".equalsIgnoreCase(schedule.getPublicScheduleYn()) && !isParticipant) {
            return ScheduleDetailResponseDto.builder()
                    .userScheduleId(schedule.getUserScheduleId())
                    .publicScheduleYn(schedule.getPublicScheduleYn())
                    .publicMemberYn(schedule.getPublicMemberYn())
                    .status(schedule.getStatus())
                    .build();
        }

        // publicMemberYn = 'N' 이고 비참여자인 경우 → 참여자 목록 마스킹
        List<ScheduleParticipantDto> participants = null;
        if ("Y".equalsIgnoreCase(schedule.getPublicMemberYn()) || isParticipant) {
            participants = userScheduleParticipantRepository
                    .findByUserScheduleId(scheduleId)
                    .stream()
                    .map(p -> new ScheduleParticipantDto(
                            p.getUserScheduleParticipantId(),
                            p.getUserId(),
                            p.getCreatedAt()))
                    .collect(Collectors.toList());
        }

        return ScheduleDetailResponseDto.builder()
                .userScheduleId(schedule.getUserScheduleId())
                .title(schedule.getTitle())
                .startAt(schedule.getStartAt())
                .endAt(schedule.getEndAt())
                .status(schedule.getStatus())
                .publicScheduleYn(schedule.getPublicScheduleYn())
                .publicMemberYn(schedule.getPublicMemberYn())
                .createdAt(schedule.getCreatedAt())
                .modifiedAt(schedule.getModifiedAt())
                .participants(participants)
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // 일정 전체 수정
    // ─────────────────────────────────────────────────────────

    /**
     * 개인 일정 전체 수정 (소유자만 가능)
     */
    @Transactional
    public UpdateScheduleResponseDto updateSchedule(Long scheduleId, Long loginedUserId,
                                                    UpdateScheduleRequestDto requestDto) {
        UserSchedule schedule = findScheduleOrThrow(scheduleId);
        checkOwnership(schedule, loginedUserId);
        validateScheduleTime(requestDto.getStartAt(), requestDto.getEndAt());

        schedule.setTitle(requestDto.getTitle());
        schedule.setStartAt(requestDto.getStartAt());
        schedule.setEndAt(requestDto.getEndAt());
        schedule.setPublicScheduleYn(requestDto.getPublicScheduleYn());
        schedule.setPublicMemberYn(requestDto.getPublicMemberYn());
        schedule.setModifiedAt(LocalDateTime.now());

        userScheduleRepository.save(schedule);
        return new UpdateScheduleResponseDto(schedule.getUserScheduleId());
    }

    // ─────────────────────────────────────────────────────────
    // 일정 상태 변경 (PATCH)
    // ─────────────────────────────────────────────────────────

    /**
     * 일정 상태만 변경 (소유자만 가능)
     */
    @Transactional
    public UpdateScheduleStatusResponseDto updateScheduleStatus(Long scheduleId, Long loginedUserId,
                                                                UserScheduleStatus newStatus) {
        UserSchedule schedule = findScheduleOrThrow(scheduleId);
        checkOwnership(schedule, loginedUserId);

        schedule.setStatus(newStatus);
        schedule.setModifiedAt(LocalDateTime.now());
        userScheduleRepository.save(schedule);

        return new UpdateScheduleStatusResponseDto(schedule.getUserScheduleId(), schedule.getStatus());
    }

    // ─────────────────────────────────────────────────────────
    // 일정 삭제
    // ─────────────────────────────────────────────────────────

    /**
     * 개인 일정 삭제 (소유자만 가능) - 참여자 매핑도 함께 삭제
     */
    @Transactional
    public void deleteSchedule(Long scheduleId, Long loginedUserId) {
        UserSchedule schedule = findScheduleOrThrow(scheduleId);
        checkOwnership(schedule, loginedUserId);

        userScheduleParticipantRepository.deleteByUserScheduleId(scheduleId);
        userScheduleRepository.delete(schedule);
    }

    // ─────────────────────────────────────────────────────────
    // 내부 유틸 메서드
    // ─────────────────────────────────────────────────────────

    private UserSchedule findScheduleOrThrow(Long scheduleId) {
        return userScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(
                        "존재하지 않는 일정입니다. scheduleId=" + scheduleId));
    }

    /**
     * 소유권 확인: userSchedule.userScheduleParticipantId → participant.userId == loginedUserId
     */
    private void checkOwnership(UserSchedule schedule, Long loginedUserId) {
        UserScheduleParticipant ownerParticipant = userScheduleParticipantRepository
                .findById(schedule.getUserScheduleParticipantId())
                .orElseThrow(() -> new ScheduleNotFoundException("일정 소유자 정보를 찾을 수 없습니다."));
        if (!ownerParticipant.getUserId().equals(loginedUserId)) {
            throw new ScheduleAccessDeniedException("해당 일정을 수정/삭제할 권한이 없습니다.");
        }
    }

    /**
     * 시작 시간이 종료 시간보다 미래인 경우 예외 발생
     */
    private void validateScheduleTime(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new InvalidScheduleTimeException("일정 시작 시간은 종료 시간보다 이전이어야 합니다.");
        }
    }
}

