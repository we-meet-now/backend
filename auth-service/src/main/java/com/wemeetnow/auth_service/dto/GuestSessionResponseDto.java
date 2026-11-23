package com.wemeetnow.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GuestSessionResponseDto {
    @Schema(description = "게스트 사용자 식별 UUID")
    private String uuId;

    @Schema(description = "Chat API 요청용 guestToken. 약 30분 후 만료됩니다.")
    private String guestToken;

    @Schema(description = "갱신 API에만 사용하는 일회성 비밀값. 응답에서 안전한 저장소로 옮긴 뒤 공유하거나 로그에 남기지 마세요.")
    private String refreshToken;

    @Schema(description = "guestToken 만료까지 남은 시간(초)")
    private long guestTokenExpiresInSeconds;

    @Schema(description = "refreshToken 유휴 만료 시각(최대 유효 기간은 발급 후 180일)")
    private Instant refreshTokenExpiresAt;
}
