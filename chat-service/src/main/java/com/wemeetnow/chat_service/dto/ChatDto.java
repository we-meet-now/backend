package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.enums.ChatType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatDto {
    private Long chatRoomId;
    private Long userId;
    private String message;
    private ChatType chatType;

}