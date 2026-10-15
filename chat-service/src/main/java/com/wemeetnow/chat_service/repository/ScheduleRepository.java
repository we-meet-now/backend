package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s WHERE s.roomId = :roomId AND (s.useYn IS NULL OR s.useYn = 'Y')")
    List<Schedule> findActiveByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT s FROM Schedule s WHERE s.scheduleId = :scheduleId " +
            "AND (s.useYn IS NULL OR s.useYn = 'Y')")
    Optional<Schedule> findActiveById(@Param("scheduleId") Long scheduleId);
}
