package com.wemeetnow.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuestBrowserSessionIdentityDto {
    @Schema(description = "게스트 사용자의 내부 ID")
    private Long userId;

    @Schema(description = "게스트 사용자 UUID. 브라우저에 전달하지 마세요.")
    private String uuid;

    @Schema(description = "게스트 사용자의 표시 이름")
    private String username;
}
