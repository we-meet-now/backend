package com.wemeetnow.auth_service.service.impl;

import com.wemeetnow.auth_service.domain.enums.FriendStatus;
import com.wemeetnow.auth_service.dto.FriendInfoDto;
import com.wemeetnow.auth_service.repository.FriendRepository;
import com.wemeetnow.auth_service.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendRepository friendRepository;

    @Override
    public List<FriendInfoDto> getFriendList(Long userId) {
        return friendRepository.getFriendList(userId) ;
    }

    @Override
    @Transactional
    public int acceptNewFriend(Long receiveUserId, Long sendUserId, FriendStatus friendStatus) {
        return friendRepository.acceptNewFriend(receiveUserId, sendUserId, friendStatus);
    }

    @Override
    @Transactional
    public int sendNewFriend(Long sendUserId, Long receiveUserId, FriendStatus friendStatus) {
        return friendRepository.sendNewFriend(sendUserId, receiveUserId, friendStatus);
    }
}
