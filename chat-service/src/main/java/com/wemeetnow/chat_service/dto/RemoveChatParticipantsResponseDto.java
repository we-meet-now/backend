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
public class RemoveChatParticipantsResponseDto {
    private Long roomId;
    private List<Long> removedUserIdList;
    private int removedCount;
}
