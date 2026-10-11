package com.wemeetnow.chat_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class InviteChatParticipantsRequestDto {
    private Long roomId;
    private List<Long> invitedUserIdList;
}
