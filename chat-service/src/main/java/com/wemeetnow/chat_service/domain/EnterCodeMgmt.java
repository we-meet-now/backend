package com.wemeetnow.chat_service.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(name = "enter_code_mgmt")
public class EnterCodeMgmt extends BaseTime {

    @Id
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Column(name = "enter_code", length = 10, nullable = true)
    private String enterCode;

    @Builder
    public EnterCodeMgmt(Long chatRoomId, String enterCode) {
        this.chatRoomId = chatRoomId;
        this.enterCode = enterCode;
    }
}
