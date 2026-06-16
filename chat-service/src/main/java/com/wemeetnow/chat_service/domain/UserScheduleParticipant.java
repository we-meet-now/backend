package com.wemeetnow.chat_service.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 개인일정 참여자 매핑 테이블
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "user_schedule_participant")
public class UserScheduleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_schedule_participant_id")
    private Long userScheduleParticipantId;

    @Column(name = "user_schedule_id")
    private Long userScheduleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

