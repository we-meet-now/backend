package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query(value = """
            SELECT cr.*
            FROM chat_room cr
            JOIN chat_participant cp 
                ON cr.chat_room_id = cp.chat_room_id
            WHERE cp.user_id = :userId  
            """, nativeQuery = true)
    List<ChatRoom> findByUserId(@Param("userId") Long userId);
}
