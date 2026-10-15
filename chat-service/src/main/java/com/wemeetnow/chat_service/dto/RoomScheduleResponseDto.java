package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.Schedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomScheduleResponseDto {
    private Long scheduleId;
    private Long roomId;
    private String planYmd;
    private String planHm;
    private String planAmpm;
    private Long storeId;
    private String meetingType;
    private Long userId;
    private String useYn;
    private LocalDateTime inpDate;
    private LocalDateTime mdfyDate;
    private String inpUserId;
    private String mdfyUserId;

    public static RoomScheduleResponseDto fromEntity(Schedule schedule) {
        return RoomScheduleResponseDto.builder()
                .scheduleId(schedule.getScheduleId())
                .roomId(schedule.getRoomId())
                .planYmd(schedule.getPlanYmd())
                .planHm(schedule.getPlanHm())
                .planAmpm(schedule.getPlanAmpm())
                .storeId(schedule.getStoreId())
                .meetingType(schedule.getMeetingType())
                .userId(schedule.getUserId())
                .useYn(schedule.getUseYn())
                .inpDate(schedule.getInpDate())
                .mdfyDate(schedule.getMdfyDate())
                .inpUserId(schedule.getInpUserId())
                .mdfyUserId(schedule.getMdfyUserId())
                .build();
    }
}
