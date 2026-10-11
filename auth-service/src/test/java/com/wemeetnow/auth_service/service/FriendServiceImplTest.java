package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.ErrorCode;
import com.wemeetnow.auth_service.domain.enums.FriendStatus;
import com.wemeetnow.auth_service.dto.FriendInfoDto;
import com.wemeetnow.auth_service.exception.CustomException;
import com.wemeetnow.auth_service.repository.FriendRepository;
import com.wemeetnow.auth_service.repository.UserRepository;
import com.wemeetnow.auth_service.service.impl.FriendServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FriendServiceImplTest {
    private final FriendRepository friendRepository = mock(FriendRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final FriendServiceImpl friendService = new FriendServiceImpl(friendRepository, userRepository);

    @Test
    void returnsFriendListWithoutDeletedRelationships() {
        List<FriendInfoDto> expected = List.of(FriendInfoDto.builder()
                .friendId(2L)
                .friendName("friend")
                .friendStatus("NEW")
                .build());
        when(friendRepository.getFriendList(1L, FriendStatus.DEL)).thenReturn(expected);

        List<FriendInfoDto> result = friendService.getFriendList(1L);

        assertThat(result).isEqualTo(expected);
        verify(friendRepository).getFriendList(1L, FriendStatus.DEL);
    }

    @Test
    void rejectsInvalidFriendListUserId() {
        assertThatThrownBy(() -> friendService.getFriendList(0L))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(friendRepository, never()).getFriendList(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void acceptsOnlyPendingFriendInvitations() {
        when(friendRepository.acceptNewFriend(1L, 2L, FriendStatus.NEW, FriendStatus.WAIT)).thenReturn(1);

        int updatedCount = friendService.acceptNewFriend(1L, 2L);

        assertThat(updatedCount).isEqualTo(1);
        verify(friendRepository).acceptNewFriend(1L, 2L, FriendStatus.NEW, FriendStatus.WAIT);
    }

    @Test
    void rejectsSelfFriendInvitationAcceptance() {
        assertThatThrownBy(() -> friendService.acceptNewFriend(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(friendRepository, never()).acceptNewFriend(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sendsInvitationsToExistingUsers() {
        when(userRepository.findAllInUserIdList(List.of(2L, 3L))).thenReturn(List.of(user(2L), user(3L)));
        when(friendRepository.findExistingFriendRelationshipUserIds(1L, List.of(2L, 3L), FriendStatus.DEL))
                .thenReturn(List.of());

        List<Long> invitedUserIds = friendService.sendFriendInvitations(1L, List.of("2", "3"));

        assertThat(invitedUserIds).containsExactly(2L, 3L);
        verify(friendRepository).sendNewFriend(1L, 2L, FriendStatus.WAIT);
        verify(friendRepository).sendNewFriend(1L, 3L, FriendStatus.WAIT);
    }

    @Test
    void rejectsAlreadyExistingFriendRelationshipWithoutSendingInvitations() {
        when(userRepository.findAllInUserIdList(List.of(2L))).thenReturn(List.of(user(2L)));
        when(friendRepository.findExistingFriendRelationshipUserIds(1L, List.of(2L), FriendStatus.DEL))
                .thenReturn(List.of(2L));

        assertThatThrownBy(() -> friendService.sendFriendInvitations(1L, List.of("2")))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_ALREADY_EXISTS);
        verify(friendRepository, never()).sendNewFriend(1L, 2L, FriendStatus.WAIT);
    }

    @Test
    void rejectsMissingTargetUsersWithoutSendingAnyInvitations() {
        when(userRepository.findAllInUserIdList(List.of(2L, 3L))).thenReturn(List.of(user(2L)));

        assertThatThrownBy(() -> friendService.sendFriendInvitations(1L, List.of("2", "3")))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(friendRepository, never()).sendNewFriend(1L, 2L, FriendStatus.WAIT);
    }

    @Test
    void rejectsMalformedInvitationRequest() {
        assertThatThrownBy(() -> friendService.sendFriendInvitations(1L, List.of("not-an-id")))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(userRepository, never()).findAllInUserIdList(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void updatesFriendStatusForAnExistingRelationship() {
        when(friendRepository.updateFriendStatus(FriendStatus.BLOCK, 1L, 2L, FriendStatus.DEL)).thenReturn(1);

        int updatedCount = friendService.updateFriendStatus(FriendStatus.BLOCK, 1L, 2L);

        assertThat(updatedCount).isEqualTo(1);
        verify(friendRepository).updateFriendStatus(FriendStatus.BLOCK, 1L, 2L, FriendStatus.DEL);
    }

    @Test
    void deletesFriendByMarkingTheRelationshipDeleted() {
        when(friendRepository.updateFriendStatus(FriendStatus.DEL, 1L, 2L, FriendStatus.DEL)).thenReturn(1);

        int updatedCount = friendService.updateFriendStatus(FriendStatus.DEL, 1L, 2L);

        assertThat(updatedCount).isEqualTo(1);
        verify(friendRepository).updateFriendStatus(FriendStatus.DEL, 1L, 2L, FriendStatus.DEL);
    }

    @Test
    void rejectsInvalidFriendStatusUpdateArguments() {
        assertThatThrownBy(() -> friendService.updateFriendStatus(FriendStatus.BLOCK, 1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(friendRepository, never()).updateFriendStatus(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private User user(Long id) {
        User user = User.builder().username("user-" + id).build();
        user.setId(id);
        return user;
    }
}
