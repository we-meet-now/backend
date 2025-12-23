package com.wemeetnow.chat_service.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "chat_room")
public class ChatRoom extends BaseTime{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "chat_room_nm")
    private String chatRoomNm;

    @Transient
    private String inpUserId;

    @Builder
    public ChatRoom(String chatRoomNm, String inpUserId) {
        this.chatRoomNm = chatRoomNm;
        super.setInpUserId(inpUserId);
    }

}