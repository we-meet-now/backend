package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.ChatParticipant;
import com.wemeetnow.chat_service.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    @Query(value = """
        SELECT cp.user_id
        FROM user u
        JOIN chat_participant cp
            ON u.user_id = cp.user_id
        WHERE cp.chat_room_id = :roomId
          AND cp.use_yn = 'Y'
          AND cp.user_id = :userId
        """, nativeQuery = true)
    List<Long> findByChatRoomId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatParticipant cp SET cp.useYn = 'N' WHERE cp.chatRoomId = :roomId AND cp.userId = :userId")
    void deleteByChatRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
    @Query("SELECT cp FROM ChatParticipant cp WHERE cp.chatRoomId = :roomId AND cp.useYn = 'Y'")
    List<ChatParticipant> findByAnonymousChatRoomId(Long roomId);
}
