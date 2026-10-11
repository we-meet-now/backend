package com.wemeetnow.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FriendInvitationRequestDto {
    private List<String> reqeustFreiendUserIdList;
}
