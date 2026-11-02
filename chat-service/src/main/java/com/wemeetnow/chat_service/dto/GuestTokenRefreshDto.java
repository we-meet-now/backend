package com.wemeetnow.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GuestTokenRefreshDto {
    private String guestToken;
    private String isRefreshed;
}
