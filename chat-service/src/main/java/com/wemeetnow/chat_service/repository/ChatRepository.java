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

    List<Chat> findByChatRoomId(Long chatRoomId);


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
