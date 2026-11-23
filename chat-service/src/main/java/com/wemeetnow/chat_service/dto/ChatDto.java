package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.enums.ChatType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class ChatDto {
    private Long chatRoomId;
    private String message;
    private ChatType chatType;

    @Override
    public String toString() {
        return String.format("chatRoomId : %d, message: %s, chatType: %s",
                chatRoomId, message, chatType);
    }
}