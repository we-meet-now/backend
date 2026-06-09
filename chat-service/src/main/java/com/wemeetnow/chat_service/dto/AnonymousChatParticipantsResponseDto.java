package com.wemeetnow.chat_service.dto;

import com.wemeetnow.chat_service.domain.ChatParticipant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousChatParticipantsResponseDto {
    private List<ChatParticipant> chatParticipantList;
    private List<ChatParticipantUserDto> userInfoList;
}

