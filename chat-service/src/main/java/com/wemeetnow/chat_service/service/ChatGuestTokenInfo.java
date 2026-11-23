package com.wemeetnow.chat_service.service;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ChatGuestTokenInfo {
    private String uuId;
    private String guestToken;
    private String refreshToken;
    private long guestTokenExpiresInSeconds;
    private Instant refreshTokenExpiresAt;
}
