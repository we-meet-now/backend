package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatRoomWithNotReadCountDto {
    private ChatRoom chatRoom;
    private int isNotReadCount;
}