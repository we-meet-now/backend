package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.UserSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserScheduleRepository extends JpaRepository<UserSchedule, Long> {

    @Query("select schedule from UserSchedule schedule " +
            "where schedule.userScheduleParticipantId in " +
            "(select participant.userScheduleParticipantId from UserScheduleParticipant participant " +
            "where participant.userId = :userId) " +
            "and schedule.startDt >= :monthStart and schedule.startDt < :nextMonthStart " +
            "order by schedule.startDt asc, schedule.startTim asc")
    List<UserSchedule> findCreatedBetween(
            @Param("userId") Long userId,
            @Param("monthStart") String monthStart,
            @Param("nextMonthStart") String nextMonthStart);

    @Query("select schedule from UserSchedule schedule " +
            "where schedule.userScheduleParticipantId in " +
            "(select participant.userScheduleParticipantId from UserScheduleParticipant participant " +
            "where participant.userId = :userId) " +
            "and schedule.publicScheduleYn = 'Y' " +
            "and schedule.startDt >= :monthStart and schedule.startDt < :nextMonthStart " +
            "order by schedule.startDt asc, schedule.startTim asc")
    List<UserSchedule> findPublicSchedulesByOwnerBetween(
            @Param("userId") Long userId,
            @Param("monthStart") String monthStart,
            @Param("nextMonthStart") String nextMonthStart);
}
