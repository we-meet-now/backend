package com.wemeetnow.chat_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterRoomResponseDto {
    @Schema(description = "신규 게스트 세션 발급 시 반환되는 UUID. Chat API 요청에는 포함하지 않습니다.")
    private String uuId;
    private String statusCode;
    private String statusMsg;
    private int markedReadCount;
    @Schema(description = "Chat API 요청용 guestToken")
    private String guestToken;
    @Schema(description = "게스트 세션 갱신 전용 비밀값. 안전한 저장소에 보관하고 일반 채팅 API에는 보내지 마세요.")
    private String refreshToken;
    @Schema(description = "guestToken 만료까지 남은 시간(초)")
    private long guestTokenExpiresInSeconds;
    @Schema(description = "refreshToken 만료 시각")
    private Instant refreshTokenExpiresAt;
    private String isRefreshed; // 토큰 갱신 여부 (Y/N)
    private Long loginedUserId;
    private List<ChatResponseDto> chatList;

    public EnterRoomResponseDto(String statusCode, String statusMsg, int markedCount) {
        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
        this.markedReadCount = markedCount;
    }
}