package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByChatRoomId(Long chatRoomId);
}
