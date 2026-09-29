package com.wemeetnow.auth_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatGuestTokenInfoDto {
    private String uuId;
    private String guestToken;
}
