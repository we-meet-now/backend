package com.wemeetnow.auth_service.service.impl;

import com.wemeetnow.auth_service.domain.enums.ErrorCode;
import com.wemeetnow.auth_service.domain.enums.FriendStatus;
import com.wemeetnow.auth_service.dto.FriendInfoDto;
import com.wemeetnow.auth_service.exception.CustomException;
import com.wemeetnow.auth_service.repository.FriendRepository;
import com.wemeetnow.auth_service.repository.UserRepository;
import com.wemeetnow.auth_service.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FriendInfoDto> getFriendList(Long userId) {
        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return friendRepository.getFriendList(userId, FriendStatus.DEL);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendInfoDto> getReceivedFriendInvitations(Long receiverId) {
        if (receiverId == null || receiverId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return friendRepository.getReceivedFriendInvitations(receiverId, FriendStatus.WAIT);
    }

    @Override
    @Transactional
    public int acceptNewFriend(Long receiveUserId, Long sendUserId) {
        if (receiveUserId == null || receiveUserId <= 0 || sendUserId == null || sendUserId <= 0
                || receiveUserId.equals(sendUserId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return friendRepository.acceptNewFriend(
                receiveUserId, sendUserId, FriendStatus.NEW, FriendStatus.WAIT);
    }

    @Override
    @Transactional
    public int sendNewFriend(Long sendUserId, Long receiveUserId, FriendStatus friendStatus) {
        return friendRepository.sendNewFriend(sendUserId, receiveUserId, friendStatus);
    }

    @Override
    @Transactional
    public List<Long> sendFriendInvitations(Long senderId, List<String> targetUserIds) {
        if (senderId == null || senderId <= 0 || targetUserIds == null || targetUserIds.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Long> parsedTargetIds = new ArrayList<>(targetUserIds.size());
        Set<Long> uniqueTargetIds = new HashSet<>();
        for (String targetUserId : targetUserIds) {
            if (targetUserId == null || targetUserId.isBlank()) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }

            long parsedTargetId;
            try {
                parsedTargetId = Long.parseLong(targetUserId);
            } catch (NumberFormatException e) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }

            if (parsedTargetId <= 0 || parsedTargetId == senderId || !uniqueTargetIds.add(parsedTargetId)) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            parsedTargetIds.add(parsedTargetId);
        }

        Set<Long> foundUserIds = userRepository.findAllInUserIdList(parsedTargetIds).stream()
                .map(user -> user.getId())
                .collect(Collectors.toSet());
        if (!foundUserIds.containsAll(parsedTargetIds)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        List<Long> existingFriendIds = friendRepository.findExistingFriendRelationshipUserIds(
                senderId, parsedTargetIds, FriendStatus.DEL);
        if (!existingFriendIds.isEmpty()) {
            throw new CustomException(ErrorCode.FRIEND_ALREADY_EXISTS);
        }

        for (Long targetUserId : parsedTargetIds) {
            friendRepository.sendNewFriend(senderId, targetUserId, FriendStatus.WAIT);
        }
        return parsedTargetIds;
    }

    @Override
    @Transactional
    public int updateFriendStatus(FriendStatus friendStatus, Long loginUserId, Long targetUserId) {
        if (friendStatus == null || loginUserId == null || loginUserId <= 0
                || targetUserId == null || targetUserId <= 0 || loginUserId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return friendRepository.updateFriendStatus(friendStatus, loginUserId, targetUserId, FriendStatus.DEL);
    }
}
