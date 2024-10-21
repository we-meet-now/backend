package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.dto.UserJoinRequestDto;
import com.wemeetnow.auth_service.dto.UserJoinResponseDto;
import com.wemeetnow.auth_service.dto.UserLoginRequestDto;
import com.wemeetnow.auth_service.dto.UserLoginResponseDto;
import com.wemeetnow.auth_service.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService{
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserJoinResponseDto join(UserJoinRequestDto joinRequestDto) {
        String email = joinRequestDto.getEmail();
        String password = joinRequestDto.getPassword();
        String passwordCorrect = joinRequestDto.getPasswordCorrect();

        userRepository.findByEmail(email).ifPresent((user) -> {
            throw new ApplicationContextException("이미 가입된 이메일입니다.");
        });

        if(!passwordCorrect.equals(passwordCorrect)){
            throw new ApplicationContextException("비밀번호가 일치하지 않습니다.");
        }
        User user = joinRequestDto.toEntity(passwordEncoder.encode(password));
        User savedUser = userRepository.save(user);
        log.info("savedUser = ", savedUser);
        UserJoinResponseDto responseDto = UserJoinResponseDto.toDto(savedUser);
        return responseDto;
    }
    public UserLoginResponseDto login(UserLoginRequestDto loginRequestDto){
        User findUser = userRepository.findByEmail(loginRequestDto.getEmail()).orElseThrow(() -> new ApplicationContextException("이메일에 존재하는 계정이 없습니다."));
        if(!passwordEncoder.matches(loginRequestDto.getPassword(), findUser.getPassword())) {
            throw new ApplicationContextException("비밀번호가 일치하지 않습니다.");
        }
        // expiration date 까지 설정해서 token return 함
        String accessToken = jwtUtil.generateAccessToken(findUser.getId(), findUser.getEmail(), findUser.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(findUser.getId(), findUser.getEmail(), findUser.getRole());

        System.out.println("===Token 출력===");
        log.info("accessToken: {}", accessToken);
        log.info("refreshToken: {}", refreshToken);

        UserLoginResponseDto responseDto = new UserLoginResponseDto(accessToken, refreshToken);
        return responseDto;
    }
    public User getUserByEmail(String email){
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("일치하는 사용자 이메일이 없습니다."));
    }
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("일치하는 사용자 id가 없습니다."));
    }
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    /**
     * 헤더에 토큰이 유효하지 않을 경우 return 0L
     * 유효할 경우 return 사용자id:Long
     * */
    public Long getUserIdFromTokenInRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        Long retValue = 0L;
        try {
            String token = authorizationHeader.replace("Bearer ", "");
            if (!JwtUtil.isExpired(token)){
                retValue = JwtUtil.getId(token);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("error: ", e.getMessage());
        }
        return retValue;
    }
    public User getUserFromTokenInRequest(HttpServletRequest request) throws Exception {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = authorizationHeader.replace("Bearer ", "");
        if (JwtUtil.isExpired(token)) {
            log.error("토큰이 유효하지 않습니다.");
            throw new Exception("토큰이 유효하지 않습니다.");
        }
        Optional<User> findUser = userRepository.findById(JwtUtil.getId(token));
        if (findUser.isPresent()) {
            return findUser.get();
        } else {
            log.error("사용자가 존재하지 않습니다.");
            throw new Exception("사용자가 존재하지 않습니다.");
        }
    }
}