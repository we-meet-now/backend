package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.enums.UserScheduleStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleListItemResponseDto {
    private Long userScheduleId;
    private String title;
    private String startDt;
    private String startTim;
    private String endDt;
    private String endTim;
    private UserScheduleStatus status;
    private String publicScheduleYn;
    private String publicMemberYn;
}
