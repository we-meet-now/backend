package com.wemeetnow.chat_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EnterRoomRequestDto {
    @Schema(
            description = "입장하고 채팅 내역을 조회할 채팅방 ID. UUID나 userId는 전달하지 않습니다.",
            example = "501",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long roomId;
}
