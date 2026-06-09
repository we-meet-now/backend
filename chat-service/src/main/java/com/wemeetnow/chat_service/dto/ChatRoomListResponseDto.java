package com.wemeetnow.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomListResponseDto {
    private Long loginedUserId;
    private List<ChatRoomWithNotReadCountDto> chatRoomList;
}

