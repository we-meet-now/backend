package com.wemeetnow.chat_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthUserDto {
    private Long userId;
    private String statusCd;
    private String statusMsg;
}
