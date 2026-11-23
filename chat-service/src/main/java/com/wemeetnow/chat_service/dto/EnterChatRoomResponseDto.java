package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.Chat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnterChatRoomResponseDto {
    @Schema(description = "인증 정보에서 서버가 확인한 사용자 ID")
    private Long loginedUserId;

    @Schema(description = "해당 roomId와 인증 사용자에 연결된 채팅 내역")
    private List<Chat> chatList;
}
