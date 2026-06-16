package com.wemeetnow.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ScheduleParticipantDto {
    private Long userScheduleParticipantId;
    private Long userId;
    private LocalDateTime createdAt;
}

