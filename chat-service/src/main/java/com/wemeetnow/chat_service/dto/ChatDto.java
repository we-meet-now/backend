package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.enums.ChatType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ChatDto {
    private Long chatRoomId;
    private Long userId;
    private String message;
    private ChatType chatType;

    public String toString() {
        return String.format("chatRoomId : %d, userId : %d, message: %s, chatType: %s",
                chatRoomId, userId, message, chatType);
    }
}