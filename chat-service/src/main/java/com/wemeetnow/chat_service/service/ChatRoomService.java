package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    public List<ChatRoom> findByUserId(Long userId) {
        return chatRoomRepository.findByUserId(userId);
    }
}
