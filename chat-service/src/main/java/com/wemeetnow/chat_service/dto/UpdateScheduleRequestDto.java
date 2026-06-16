package com.wemeetnow.chat_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateScheduleRequestDto {
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String publicScheduleYn;
    private String publicMemberYn;
}

