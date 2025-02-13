package com.wemeetnow.auth_service.domain;

import com.wemeetnow.auth_service.domain.enums.FriendStatus;
import com.wemeetnow.auth_service.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.lang.Nullable;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Table(name = "friend")
public class Friend extends BaseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friend_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // foreign key 설정(친구: 사용자 = N:1, 사용자 1명이 여려명의 친구만들 수 있음)
    private User user;

    @Column(name = "sender_id")
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "friend_status")
    private FriendStatus friendStatus;
}
