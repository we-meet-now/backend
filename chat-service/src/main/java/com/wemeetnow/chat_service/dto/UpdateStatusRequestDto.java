package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.enums.UserScheduleStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequestDto {
    private UserScheduleStatus status;
}

