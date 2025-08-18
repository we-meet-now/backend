package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.domain.enums.FriendStatus;
import com.wemeetnow.auth_service.dto.FriendInfoDto;

import java.util.List;
import java.util.Map;

public interface FriendService {
    List<FriendInfoDto> getFriendList(Long userId);

    int acceptNewFriend(Long receiveUserId, Long sendUserId, FriendStatus friendStatus);

    int sendNewFriend(Long sendUserId, Long receiveUserId, FriendStatus friendStatus);

    int updateFriendStatus(FriendStatus statusType, Long loginUserId, Long targetUserId);
}
