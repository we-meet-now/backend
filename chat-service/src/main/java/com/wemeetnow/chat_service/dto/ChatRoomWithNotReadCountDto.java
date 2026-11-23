package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatRoomWithNotReadCountDto {
    @Schema(description = "채팅방 정보")
    private ChatRoom chatRoom;

    @Schema(description = "현재 사용자가 읽지 않은 메시지 수", example = "2")
    private int isNotReadCount;
}