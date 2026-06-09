package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.Chat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnterChatRoomResponseDto {
    private Long loginedUserId;
    private List<Chat> chatList;
}

