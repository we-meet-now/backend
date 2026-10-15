package com.wemeetnow.auth_service.dto;

import com.wemeetnow.auth_service.domain.Friend;
import com.wemeetnow.auth_service.domain.enums.FriendStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FriendTableRowDto {
    private Long friendId;
    private Long userId;
    private Long senderId;
    private FriendStatus friendStatus;
    private LocalDateTime inpDate;
    private LocalDateTime mdfyDate;
    private String inpUserId;
    private String mdfyUserId;

    public static FriendTableRowDto fromEntity(Friend friend) {
        return FriendTableRowDto.builder()
                .friendId(friend.getId())
                .userId(friend.getUser().getId())
                .senderId(friend.getSenderId())
                .friendStatus(friend.getFriendStatus())
                .inpDate(friend.getInpDate())
                .mdfyDate(friend.getMdfyDate())
                .inpUserId(friend.getInpUserId())
                .mdfyUserId(friend.getMdfyUserId())
                .build();
    }
}
