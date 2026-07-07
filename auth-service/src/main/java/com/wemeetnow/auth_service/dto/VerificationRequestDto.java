package com.wemeetnow.auth_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificationRequestDto {
    private String email;
    private String code;
}