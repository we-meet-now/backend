package com.wemeetnow.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SetUsernameResponseDto {
    private boolean success;
    private String username;
}
