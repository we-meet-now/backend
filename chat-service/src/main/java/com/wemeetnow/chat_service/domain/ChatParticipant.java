package com.wemeetnow.chat_service.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "chat_participant")
public class ChatParticipant extends BaseTime{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_participant_id")
    private Long chatParticipantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Column(name = "use_yn", length = 1, nullable = false)
    @ColumnDefault("'Y'")
    private char useYn;
}
