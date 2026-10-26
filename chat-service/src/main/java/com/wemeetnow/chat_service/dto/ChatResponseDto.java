package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.enums.ChatType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatResponseDto {
    private Long chatId;
    private Long chatRoomId;
    private Long userId;
    @Schema(description = "사용자 테이블의 username. 프로필이 없거나 username이 미설정이면 null")
    private String username;
    private String message;
    private ChatType chatType;
    private LocalDateTime inpDate;

    @Builder
    public ChatResponseDto(
            Long chatId, Long chatRoomId, Long userId, String username,
            String message, ChatType chatType, LocalDateTime inpDate) {
        this.chatId = chatId;
        this.chatRoomId = chatRoomId;
        this.userId = userId;
        this.username = username;
        this.message = message;
        this.chatType = chatType;
        this.inpDate = inpDate;
    }

    public static ChatResponseDto fromEntity(Chat chat) {
        return fromEntity(chat, null);
    }

    public static ChatResponseDto fromEntity(Chat chat, String username) {
        return ChatResponseDto.builder()
                .chatId(chat.getChatId())
                .chatRoomId(chat.getChatRoomId())
                .userId(chat.getUserId())
                .username(username)
                .message(chat.getMessage())
                .chatType(chat.getChatType())
                .inpDate(chat.getInpDate())
                .build();
    }
}