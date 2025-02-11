package com.wemeetnow.auth_service.service.impl;

import com.wemeetnow.auth_service.dto.FriendInfoDto;
import com.wemeetnow.auth_service.service.FriendService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendServiceImpl implements FriendService {

    @Override
    public List<FriendInfoDto> getFriendList(Long userId) {
        return null;
    }
}
