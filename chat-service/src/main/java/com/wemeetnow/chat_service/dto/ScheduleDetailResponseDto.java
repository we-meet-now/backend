package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.enums.UserScheduleStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ScheduleDetailResponseDto {
    private Long userScheduleId;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private UserScheduleStatus status;
    private String publicScheduleYn;
    private String publicMemberYn;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    /** publicMemberYn = 'N' 이고 비참여자인 경우 null 처리 */
    private List<ScheduleParticipantDto> participants;
}

