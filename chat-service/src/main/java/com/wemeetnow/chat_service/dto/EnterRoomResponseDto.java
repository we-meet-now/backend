package com.wemeetnow.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterRoomResponseDto {
    private String statusCode;
    private String statusMsg;
    private int markedReadCount;
    private String guestToken;

    public EnterRoomResponseDto(String statusCode, String statusMsg, int markedCount) {
        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
        this.markedReadCount = markedCount;
    }
}