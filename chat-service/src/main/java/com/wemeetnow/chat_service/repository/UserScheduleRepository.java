package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.UserSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserScheduleRepository extends JpaRepository<UserSchedule, Long> {
}

