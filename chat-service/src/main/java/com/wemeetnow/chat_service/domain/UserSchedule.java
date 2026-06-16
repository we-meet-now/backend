package com.wemeetnow.chat_service.domain;

import com.wemeetnow.chat_service.domain.enums.UserScheduleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 개인 일정 정보 테이블
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "user_schedule")
public class UserSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_schedule_id")
    private Long userScheduleId;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private UserScheduleStatus status;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "public_schedule_yn", length = 1)
    private String publicScheduleYn;

    @Column(name = "public_member_yn", length = 1)
    private String publicMemberYn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "user_schedule_participant_id", nullable = false)
    private Long userScheduleParticipantId;
}

