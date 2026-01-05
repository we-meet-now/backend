package com.wemeetnow.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class RandomNicknameResponseDto {
    private String nickname;
}
