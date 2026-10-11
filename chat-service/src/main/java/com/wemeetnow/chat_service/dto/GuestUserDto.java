package com.wemeetnow.chat_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * auth-service의 /api/auth/v1/users/get-guest-id 응답(data)을 매핑하기 위한 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class GuestUserDto {
    private Long userId;
    private String uuId;
    private String statusCd;
    private String statusMsg;
    private String isGuestYn;
    private String guestToken;
    private String isRefreshed; // 토큰 갱신 여부 (Y/N)
}
