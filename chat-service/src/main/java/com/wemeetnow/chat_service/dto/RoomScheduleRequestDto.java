package com.wemeetnow.chat_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomScheduleRequestDto {
    private String planYmd;
    private String planHm;
    private String planAmpm;
    private Long storeId;
    private String meetingType;
}
