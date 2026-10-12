package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.domain.enums.FriendStatus;
import com.wemeetnow.auth_service.dto.FriendInfoDto;

import java.util.List;

public interface FriendService {
    List<FriendInfoDto> getFriendList(Long userId);

    List<FriendInfoDto> getReceivedFriendInvitations(Long receiverId);

    int acceptNewFriend(Long receiveUserId, Long sendUserId);

    int sendNewFriend(Long sendUserId, Long receiveUserId, FriendStatus friendStatus);

    List<Long> sendFriendInvitations(Long senderId, List<String> targetUserIds);

    int updateFriendStatus(FriendStatus statusType, Long loginUserId, Long targetUserId);
}
