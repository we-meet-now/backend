package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.domain.ChatParticipant;
import com.wemeetnow.chat_service.repository.ChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatParticipantService {
    private final ChatParticipantRepository chatParticipantRepository;

    public List<Long> findByChatRoomId(Long roomId, Long userId) {
        return chatParticipantRepository.findByChatRoomId(roomId, userId); }
}
