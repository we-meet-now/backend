package com.wemeetnow.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FriendStatusUpdateRequestDto {
    private Long targetUserId;
    private String friendStatus;
}
