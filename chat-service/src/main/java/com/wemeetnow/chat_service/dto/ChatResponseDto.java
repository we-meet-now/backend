package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.enums.ChatType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatResponseDto {
    private Long chatId;
    private Long chatRoomId;
    private Long userId;
    private String message;
    private ChatType chatType;
    private LocalDateTime inpDate;

    @Builder
    public ChatResponseDto(Long chatId, Long chatRoomId, Long userId, String message, ChatType chatType, LocalDateTime inpDate) {
        this.chatId = chatId;
        this.chatRoomId = chatRoomId;
        this.userId = userId;
        this.message = message;
        this.chatType = chatType;
        this.inpDate = inpDate;
    }

    // 엔티티를 DTO로 변환하는 정적 팩토리 메서드
    public static ChatResponseDto fromEntity(Chat chat) {
        return ChatResponseDto.builder()
                .chatId(chat.getChatId())
                .chatRoomId(chat.getChatRoomId())
                .userId(chat.getUserId())
                .message(chat.getMessage())
                .chatType(chat.getChatType())
                .inpDate(chat.getInpDate()) // Auditing으로 생성된 시간
                .build();
    }
}