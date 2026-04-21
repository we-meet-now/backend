package com.wemeetnow.chat_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateChatRoomRequestDto {
    String meetTime;
    List<Long> participantIds;
}
