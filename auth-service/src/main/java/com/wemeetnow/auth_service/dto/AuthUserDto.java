package com.wemeetnow.auth_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthUserDto {
    private Long userId;
    private String statusCd;
    private String statusMsg;
}

