package com.wemeetnow.chat_service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterRoomResponseDto {
    private String uuId;
    private String statusCode;
    private String statusMsg;
    private int markedReadCount;
    private String guestToken;
    private String isRefreshed; // 토큰 갱신 여부 (Y/N)
    private Long loginedUserId;
    private List<ChatResponseDto> chatList;

    public EnterRoomResponseDto(String statusCode, String statusMsg, int markedCount) {
        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
        this.markedReadCount = markedCount;
    }
}