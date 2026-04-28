package com.wemeetnow.chat_service.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "schedule")
public class Schedule extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "plan_ymd", length = 6)
    private String planYmd;

    @Column(name = "plan_hm", length = 6)
    private String planHm;

    @Column(name = "plan_ampm", length = 4)
    private String planAmpm;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "meeting_type", length = 20)
    private String meetingType;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "use_yn", length = 2)
    private String useYn;
}