package com.wemeetnow.chat_service.domain;

import com.wemeetnow.chat_service.domain.enums.ChatType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(length = 1024)
    private String message;

    @Column(name = "not_read_count")
    private Integer notReadCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_type")
    private ChatType chatType;

    @Column(name = "update_yn", length = 1, nullable = false)
    @ColumnDefault("'N'")
    private char updateYn;

    @Column(name = "delete_yn", length = 1, nullable = false)
    @ColumnDefault("'N'")
    private char deleteYn;
}
