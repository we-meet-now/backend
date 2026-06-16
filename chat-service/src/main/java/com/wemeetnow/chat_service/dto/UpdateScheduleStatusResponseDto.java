package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.enums.UserScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateScheduleStatusResponseDto {
    private Long userScheduleId;
    private UserScheduleStatus status;
}

