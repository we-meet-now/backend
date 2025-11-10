package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.ChatParticipant;
import com.wemeetnow.chat_service.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    @Query(value = """
            SELECT cp.*
            FROM user u
            JOIN chat_participant cp 
                ON u.user_id = cp.user_id
            WHERE cp.chat_room_id = :chatRoomId  
            """, nativeQuery = true)
    List<ChatParticipant> findByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
