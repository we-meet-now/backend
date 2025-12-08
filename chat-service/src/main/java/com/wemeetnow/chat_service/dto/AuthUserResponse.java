package com.wemeetnow.chat_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthUserResponse {
    private Long userId;      // 사용자 고유 ID
    private String username;  // 사용자 이름
    private String email;
}