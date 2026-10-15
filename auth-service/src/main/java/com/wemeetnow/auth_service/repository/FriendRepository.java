package com.wemeetnow.auth_service.repository;

import com.wemeetnow.auth_service.domain.Friend;
import com.wemeetnow.auth_service.domain.enums.FriendStatus;
import com.wemeetnow.auth_service.dto.FriendInfoDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {
    @Query("SELECT f FROM Friend f JOIN FETCH f.user")
    List<Friend> findAllWithUsers();

    @Query("SELECT new com.wemeetnow.auth_service.dto.FriendInfoDto(" +
            "CASE WHEN f.senderId = :userId THEN f.user.id ELSE u.id END, " +
            "CASE WHEN f.senderId = :userId THEN f.user.username ELSE u.username END, " +
            "CASE WHEN f.senderId = :userId THEN f.user.imgUrl ELSE u.imgUrl END, " +
            "CAST(f.friendStatus AS string)) " +
            "FROM Friend f " +
            "LEFT JOIN User u ON u.id = f.senderId " +
            "WHERE f.senderId IS NOT NULL " +
            "AND (f.senderId = :userId OR f.user.id = :userId) " +
            "AND (f.friendStatus IS NULL OR f.friendStatus <> :deletedStatus)")
    List<FriendInfoDto> getFriendList(@Param("userId") Long userId,
                                      @Param("deletedStatus") FriendStatus deletedStatus);

    @Query("SELECT new com.wemeetnow.auth_service.dto.FriendInfoDto(" +
            "inviter.id, inviter.username, inviter.imgUrl, CAST(f.friendStatus AS string)) " +
            "FROM Friend f " +
            "JOIN User inviter ON inviter.id = f.senderId " +
            "WHERE f.user.id = :receiverId AND f.friendStatus = :pendingStatus " +
            "ORDER BY f.inpDate DESC")
    List<FriendInfoDto> getReceivedFriendInvitations(@Param("receiverId") Long receiverId,
                                                     @Param("pendingStatus") FriendStatus pendingStatus);

    @Modifying
    @Transactional
    @Query("UPDATE Friend f SET f.friendStatus = :friendStatus " +
            "WHERE f.user.id = :receiveUserId AND f.senderId = :sendUserId " +
            "AND f.friendStatus = :pendingStatus")
    int acceptNewFriend(@Param("receiveUserId") Long receiveUserId,
                        @Param("sendUserId") Long sendUserId,
                        @Param("friendStatus") FriendStatus friendStatus,
                        @Param("pendingStatus") FriendStatus pendingStatus);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO friend(sender_id, user_id, friend_status) VALUES(:sendUserId, :receiveUserId, :#{#friendStatus.name()} )", nativeQuery = true)
    int sendNewFriend(@Param("sendUserId") Long sendUserId,
                      @Param("receiveUserId") Long receiveUserId,
                      @Param("friendStatus") FriendStatus friendStatus);

    @Query("SELECT DISTINCT CASE WHEN f.senderId = :senderId THEN f.user.id ELSE f.senderId END " +
            "FROM Friend f " +
            "WHERE ((f.senderId = :senderId AND f.user.id IN :targetUserIds) " +
            "OR (f.user.id = :senderId AND f.senderId IN :targetUserIds)) " +
            "AND (f.friendStatus IS NULL OR f.friendStatus <> :deletedStatus)")
    List<Long> findExistingFriendRelationshipUserIds(@Param("senderId") Long senderId,
                                                      @Param("targetUserIds") List<Long> targetUserIds,
                                                      @Param("deletedStatus") FriendStatus deletedStatus);

    @Modifying
    @Transactional
    @Query("UPDATE Friend f SET f.friendStatus = :friendStatus " +
            "WHERE ((f.user.id = :loginUserId AND f.senderId = :targetUserId) " +
            "OR (f.senderId = :loginUserId AND f.user.id = :targetUserId)) " +
            "AND (f.friendStatus IS NULL OR f.friendStatus <> :deletedStatus)")
    int updateFriendStatus(@Param("friendStatus") FriendStatus friendStatus,
                           @Param("loginUserId") Long loginUserId,
                           @Param("targetUserId") Long targetUserId,
                           @Param("deletedStatus") FriendStatus deletedStatus);
}
