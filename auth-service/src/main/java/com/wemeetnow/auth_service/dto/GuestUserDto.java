package com.wemeetnow.auth_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GuestUserDto {
    private Long userId;
    private String uuId;
    private String statusCd;
    private String statusMsg;
    private String isGuestYn;
    private String guestToken;
    private String isRefreshed; // 토큰 갱신 여부 (Y/N)
}
