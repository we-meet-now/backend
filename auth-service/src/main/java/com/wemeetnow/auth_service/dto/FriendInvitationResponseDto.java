package com.wemeetnow.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FriendInvitationResponseDto {
    private int invitedCount;
    private List<Long> invitedUserIdList;
}
