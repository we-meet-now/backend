package com.wemeetnow.chat_service.dto;

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
public class ChatRoomListResponseDto {
    @Schema(description = "AccessToken, guestToken 또는 guest_session에서 서버가 확인한 사용자 ID")
    private Long loginedUserId;

    @Schema(description = "인증된 사용자가 참여한 채팅방 목록과 각 방의 미읽음 메시지 수")
    private List<ChatRoomWithNotReadCountDto> chatRoomList;
}
