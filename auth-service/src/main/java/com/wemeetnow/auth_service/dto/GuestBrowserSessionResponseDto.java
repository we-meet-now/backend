package com.wemeetnow.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GuestBrowserSessionResponseDto {
    @Schema(description = "게스트 세션의 유휴 만료 시각")
    private Instant expiresAt;

    @Schema(description = "게스트 세션의 절대 만료 시각")
    private Instant absoluteExpiresAt;
}
