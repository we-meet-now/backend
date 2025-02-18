package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.dto.FriendInfoDto;

import java.util.List;

public interface FriendService {
    List<FriendInfoDto> getFriendList(Long userId);

    int acceptNewFriend(Long receiveUserId, Long sendUserId);
}
