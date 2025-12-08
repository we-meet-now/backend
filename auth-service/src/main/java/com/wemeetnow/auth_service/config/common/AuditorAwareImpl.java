package com.wemeetnow.auth_service.config.common;

import com.wemeetnow.auth_service.config.auth.PrincipalDetails;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<String> {

    @Autowired
    private final PrincipalDetails principalDetails;

    @Override
    public Optional<String> getCurrentAuditor() {

        String userName = principalDetails == null ? "system" : principalDetails.getUsername();

        return Optional.of(userName);
    }
}