package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.UserScheduleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserScheduleParticipantRepository extends JpaRepository<UserScheduleParticipant, Long> {

    /** 특정 일정에 속한 모든 참여자 조회 */
    List<UserScheduleParticipant> findByUserScheduleId(Long userScheduleId);

    /** 특정 일정에 특정 유저가 참여자인지 확인 */
    boolean existsByUserScheduleIdAndUserId(Long userScheduleId, Long userId);

    /** 특정 일정의 모든 참여자 삭제 */
    void deleteByUserScheduleId(Long userScheduleId);
}

