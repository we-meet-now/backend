package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatReadRepository extends JpaRepository<ChatRead, Long> {

    boolean existsByChatIdAndUserId(Long chatId, Long userId);
}
