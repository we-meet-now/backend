package com.wemeetnow.chat_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateScheduleRequestDto {
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String publicScheduleYn;
    private String publicMemberYn;
    /** 초대할 참여자 userId 목록 (생성자 제외) */
    private List<Long> participantIds;
}

