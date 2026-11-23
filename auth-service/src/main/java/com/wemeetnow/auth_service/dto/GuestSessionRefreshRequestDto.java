package com.wemeetnow.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GuestSessionRefreshRequestDto {
    @Schema(description = "현재 보유한 refreshToken. 갱신 또는 폐기 API에서만 전달합니다.", example = "<저장된 refreshToken>")
    private String refreshToken;
}
