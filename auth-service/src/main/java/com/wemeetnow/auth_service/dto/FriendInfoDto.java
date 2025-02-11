package com.wemeetnow.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendInfoDto {
    private String fiendId;
    private String friendName;
    private String friendImgUrl;
    private String friendStatus;
}
