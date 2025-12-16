package com.wemeetnow.chat_service.dto;

import lombok.Data;

@Data
public class ChatParticipantUserDto {
    private Long userId;
    private String username;
    private String email;
    private String imgUrl;
    private String nickname;
    private String phoneNumber;
}