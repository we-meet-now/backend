package com.wemeetnow.chat_service.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatUserInfo {
    private Long userId;
    private String username;
    private String email;
    private String imgUrl;
    private String nickname;
    private String phoneNumber;
}
