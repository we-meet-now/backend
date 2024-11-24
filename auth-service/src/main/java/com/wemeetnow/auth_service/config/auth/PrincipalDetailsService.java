package com.wemeetnow.auth_service.config.auth;

import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PrincipalDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User findUser = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("가입되지 않은 이메일입니다."));
        List<GrantedAuthority> roles = new ArrayList<>();
        log.info("findUser.getRoles(): ", findUser.getRole());
        log.info("findUser.getRoles().toString(): ", findUser.getRole().toString());
        roles.add(new SimpleGrantedAuthority(findUser.getRole().toString()));
        return PrincipalDetails.builder()
                .user(findUser)
                .build();
    }
}
