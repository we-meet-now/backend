package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.domain.Schedule;
import com.wemeetnow.chat_service.dto.RoomScheduleRequestDto;
import com.wemeetnow.chat_service.dto.RoomScheduleResponseDto;
import com.wemeetnow.chat_service.exception.InvalidScheduleRequestException;
import com.wemeetnow.chat_service.exception.ScheduleAccessDeniedException;
import com.wemeetnow.chat_service.exception.ScheduleNotFoundException;
import com.wemeetnow.chat_service.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ChatRoomService chatRoomService;

    public List<RoomScheduleResponseDto> getSchedules(Long roomId, Long userId) {
        checkRoomMembership(roomId, userId);
        return scheduleRepository.findActiveByRoomId(roomId).stream()
                .map(RoomScheduleResponseDto::fromEntity)
                .toList();
    }

    public RoomScheduleResponseDto getSchedule(Long roomId, Long scheduleId, Long userId) {
        checkRoomMembership(roomId, userId);
        return RoomScheduleResponseDto.fromEntity(findActiveSchedule(roomId, scheduleId));
    }

    @Transactional
    public RoomScheduleResponseDto createSchedule(
            Long roomId, Long userId, RoomScheduleRequestDto request) {
        checkRoomMembership(roomId, userId);
        validateRequest(request);

        Schedule schedule = Schedule.builder()
                .roomId(roomId)
                .planYmd(request.getPlanYmd())
                .planHm(request.getPlanHm())
                .planAmpm(request.getPlanAmpm())
                .storeId(request.getStoreId())
                .meetingType(request.getMeetingType())
                .userId(userId)
                .useYn("Y")
                .build();
        return RoomScheduleResponseDto.fromEntity(scheduleRepository.save(schedule));
    }

    @Transactional
    public RoomScheduleResponseDto updateSchedule(
            Long roomId, Long scheduleId, Long userId, RoomScheduleRequestDto request) {
        checkRoomMembership(roomId, userId);
        validateRequest(request);
        Schedule schedule = findActiveSchedule(roomId, scheduleId);

        schedule.setPlanYmd(request.getPlanYmd());
        schedule.setPlanHm(request.getPlanHm());
        schedule.setPlanAmpm(request.getPlanAmpm());
        schedule.setStoreId(request.getStoreId());
        schedule.setMeetingType(request.getMeetingType());
        return RoomScheduleResponseDto.fromEntity(scheduleRepository.save(schedule));
    }

    @Transactional
    public void deleteSchedule(Long roomId, Long scheduleId, Long userId) {
        checkRoomMembership(roomId, userId);
        Schedule schedule = findActiveSchedule(roomId, scheduleId);
        schedule.setUseYn("N");
        scheduleRepository.save(schedule);
    }

    private Schedule findActiveSchedule(Long roomId, Long scheduleId) {
        if (scheduleId == null || scheduleId <= 0) {
            throw new InvalidScheduleRequestException("scheduleId는 양수여야 합니다.");
        }
        Schedule schedule = scheduleRepository.findActiveById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(
                        "존재하지 않는 일정입니다. scheduleId=" + scheduleId));
        if (!roomId.equals(schedule.getRoomId())) {
            throw new ScheduleNotFoundException("해당 채팅방에서 일정을 찾을 수 없습니다.");
        }
        return schedule;
    }

    private void checkRoomMembership(Long roomId, Long userId) {
        if (roomId == null || roomId <= 0 || userId == null || userId <= 0) {
            throw new InvalidScheduleRequestException("roomId와 userId는 양수여야 합니다.");
        }
        if (!chatRoomService.isMember(roomId, userId)) {
            throw new ScheduleAccessDeniedException("채팅방 참여자만 일정을 조회하거나 관리할 수 있습니다.");
        }
    }

    private void validateRequest(RoomScheduleRequestDto request) {
        if (request == null) {
            throw new InvalidScheduleRequestException("요청 데이터가 비어 있습니다.");
        }
        checkMaxLength(request.getPlanYmd(), 6, "planYmd");
        checkMaxLength(request.getPlanHm(), 6, "planHm");
        checkMaxLength(request.getPlanAmpm(), 4, "planAmpm");
        checkMaxLength(request.getMeetingType(), 20, "meetingType");
        if (request.getStoreId() != null && request.getStoreId() <= 0) {
            throw new InvalidScheduleRequestException("storeId는 양수여야 합니다.");
        }
    }

    private void checkMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new InvalidScheduleRequestException(fieldName + "은(는) " + maxLength + "자 이하여야 합니다.");
        }
    }
}
