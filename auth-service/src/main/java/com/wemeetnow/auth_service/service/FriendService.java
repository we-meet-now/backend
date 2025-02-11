package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.dto.FriendInfoDto;

import java.util.List;

public interface FriendService {
    List<FriendInfoDto> getFriendList(Long userId);
}
