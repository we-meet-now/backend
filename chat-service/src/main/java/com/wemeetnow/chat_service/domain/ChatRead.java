package com.wemeetnow.chat_service.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_read")
public class ChatRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_read_id")
    private Long chatReadId;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "user_id")
    private Long userId;

    public ChatRead(Long chatId, Long userId) {
        this.chatId = chatId;
        this.userId = userId;
    }
}