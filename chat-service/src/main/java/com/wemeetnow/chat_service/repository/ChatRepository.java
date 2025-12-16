package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT c FROM Chat c " +
            "JOIN ChatParticipant cp ON c.chatRoomId = cp.chatRoomId " +
            "WHERE c.chatRoomId = :roomId " +
            "AND cp.userId = :userId " +
            "AND cp.useYn = 'Y'")
    List<Chat> findByChatRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);


    @Query("SELECT c FROM Chat c WHERE c.chatRoomId = :roomId " +
            "AND c.inpDate <= :now " +
            "AND c.chatId NOT IN (" +
            "   SELECT cr.chatId FROM ChatRead cr WHERE cr.userId = :userId" +
            ")")
    List<Chat> findUnreadChatsForUser(@Param("roomId") Long roomId,
                                      @Param("userId") Long userId,
                                      @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(c) FROM Chat c WHERE c.chatRoomId = :roomId AND c.chatId NOT IN (" +
            "SELECT cr.chatId FROM ChatRead cr WHERE cr.userId = :userId)")
    int countNotReadByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
