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
    @Schema(description = "처리 결과 코드. 2000은 입장 성공, 2030은 신규 게스트 자격증명 발급입니다.", example = "2000")
    private String statusCode;
    @Schema(description = "처리 결과 메시지", example = "success")
    private String statusMsg;
    @Schema(description = "이번 요청에서 읽음 처리한 메시지 수", example = "2")
    private int markedReadCount;
    @Schema(description = "Chat API 요청용 guestToken")
    private String guestToken;
    @Schema(description = "게스트 세션 갱신 전용 비밀값. 안전한 저장소에 보관하고 일반 채팅 API에는 보내지 마세요.")
    private String refreshToken;
    @Schema(description = "guestToken 만료까지 남은 시간(초)")
    private long guestTokenExpiresInSeconds;
    @Schema(description = "refreshToken 만료 시각")
    private Instant refreshTokenExpiresAt;
    @Schema(description = "guestToken을 요청 처리 중 갱신했는지 여부 (Y/N)")
    private String isRefreshed;
    @Schema(description = "인증 정보에서 서버가 확인한 사용자 ID")
    private Long loginedUserId;
    @Schema(description = "입장 성공 시 채팅 내역. 신규 게스트 자격증명 발급 응답에서는 생략됩니다.")
    private List<ChatResponseDto> chatList;

    public EnterRoomResponseDto(String statusCode, String statusMsg, int markedCount) {
        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
        this.markedReadCount = markedCount;
    }
}