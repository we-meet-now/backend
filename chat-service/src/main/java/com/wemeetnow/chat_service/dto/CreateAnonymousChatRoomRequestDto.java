package com.wemeetnow.chat_service.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAnonymousChatRoomRequestDto {
    private String chatRoomNm;
}
