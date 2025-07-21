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
    // TODO 쿼리 수정 필요(USER와 FRIEND 테이블 조인하여 FriendInfoDto에 맞게 수정 필요함)
    @Query("SELECT f FROM Friend f WHERE f.id = :userId")
    List<FriendInfoDto> getFriendList(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Friend f SET f.friendStatus = :friendStatus WHERE f.user.id = :receiveUserId AND f.senderId = :sendUserId")
    int acceptNewFriend(@Param("receiveUserId") Long receiveUserId,
                        @Param("sendUserId") Long sendUserId,
                        @Param("friendStatus") FriendStatus friendStatus);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO friend(sender_id, user_id, friend_status) VALUE(:sendUserId, :receiveUserId, :#{#friendStatus.name()} )", nativeQuery = true)
    int sendNewFriend(@Param("sendUserId") Long sendUserId,
                      @Param("receiveUserId") Long receiveUserId,
                      @Param("friendStatus") FriendStatus friendStatus);
}
