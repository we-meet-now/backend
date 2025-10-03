package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.domain.enums.ChatType;
import com.wemeetnow.chat_service.repository.ChatRepository;
import com.wemeetnow.chat_service.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public ChatRoom createRoom(String name) {
        ChatRoom room = ChatRoom.builder()
                .chatRoomNm(name)
                .build();
        return chatRoomRepository.save(room);
    }

    public ChatRoom getRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다. id=" + roomId));
    }

    @Transactional
    public Chat saveMessage(Long roomId, Long userId, String message, ChatType chatType) {
        Chat chat = Chat.builder()
                .chatRoomId(roomId)
                .userId(userId)
                .message(message)
                .chatType(chatType)
                .notReadCount(0)
                .build();
        return chatRepository.save(chat);
    }

    public List<Chat> getChatList(Long roomId) {
        return chatRepository.findByChatRoomId(roomId);
    }
}
