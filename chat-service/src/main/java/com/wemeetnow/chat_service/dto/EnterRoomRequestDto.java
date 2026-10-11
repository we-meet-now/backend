package com.wemeetnow.chat_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EnterRoomRequestDto {
    private Long roomId;
    @JsonAlias("uuid") // 클라이언트가 "uuid"(소문자)로 보내는 경우도 허용
    private String uuId;
}
