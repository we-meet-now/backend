package com.wemeetnow.auth_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatParticipantUserDto {
    private Long userId;
    private String username;
    private String email;
    private String imgUrl;
    private String nickname;
    private String phoneNumber;
}