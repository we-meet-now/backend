package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.UserSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserScheduleRepository extends JpaRepository<UserSchedule, Long> {

    @Query("select schedule from UserSchedule schedule " +
            "where schedule.userScheduleParticipantId in " +
            "(select participant.userScheduleParticipantId from UserScheduleParticipant participant " +
            "where participant.userId = :userId) " +
            "and schedule.startAt >= :monthStart and schedule.startAt < :nextMonthStart " +
            "order by schedule.startAt asc")
    List<UserSchedule> findCreatedBetween(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("nextMonthStart") LocalDateTime nextMonthStart);

    @Query("select schedule from UserSchedule schedule " +
            "where schedule.userScheduleParticipantId in " +
            "(select participant.userScheduleParticipantId from UserScheduleParticipant participant " +
            "where participant.userId = :userId) " +
            "and schedule.publicScheduleYn = 'Y' " +
            "and schedule.startAt >= :monthStart and schedule.startAt < :nextMonthStart " +
            "order by schedule.startAt asc")
    List<UserSchedule> findPublicSchedulesByOwnerBetween(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("nextMonthStart") LocalDateTime nextMonthStart);
}
