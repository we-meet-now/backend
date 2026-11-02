package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.domain.UserLog;
import com.wemeetnow.auth_service.repository.UserLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserLogPersistenceService {

    private final UserLogRepository userLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(UserLog userLog) {
        userLogRepository.save(userLog);
    }
}
