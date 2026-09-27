package com.wemeetnow.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CreateOneChatRoomResponseDto {
    private Long chatRoomId;
    private String enterCode;
}
