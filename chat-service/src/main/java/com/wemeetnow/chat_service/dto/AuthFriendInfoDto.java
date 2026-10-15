package com.wemeetnow.chat_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthFriendInfoDto {
    private Long friendId;
    private String friendName;
    private String friendImgUrl;
    private String friendStatus;
}
