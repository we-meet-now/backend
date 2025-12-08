package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.dto.*;
import com.wemeetnow.auth_service.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserApiController {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/join")
    public ResponseEntity<UserJoinResponseDto> join(@RequestBody UserJoinRequestDto requestDto) {
        UserJoinResponseDto responseDto = userService.join(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(@RequestBody UserLoginRequestDto requestDto){
        log.info("UserLoginRequestDto = [{}]", requestDto);
        UserLoginResponseDto responseDto = userService.login(requestDto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseDto);
    }
    @GetMapping("/check-is-logined")
    public ResponseEntity checkIsLogined(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("request = " + request);
        try {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            String token = authorizationHeader.replace("Bearer ", "");
            if (JwtUtil.isExpired(token)) {
                return new ResponseEntity(HttpStatus.UNAUTHORIZED);
            }
            Claims claims = JwtUtil.extractAllClaims(token);
            // JwtUtil.getEmail(token);
            log.info("claims.get(\"email\") = " + claims.get("email"));
            // JwtUtil.getId(token);
            log.info("claims.get(\"userId\") = " + claims.get("userId"));
            return new ResponseEntity(claims, HttpStatus.OK);
        } catch (Exception e) {
            log.error("raised error: ", e);
            throw new Exception(e);
        }
    }
    @GetMapping("/all")
    public ResponseEntity getUsersAll() {
        List<User> allUsers = userService.getAllUsers();
        return new ResponseEntity(allUsers, HttpStatus.OK);
    }

    @GetMapping("/get-id")
    public ResponseEntity<AuthUserDto> getUserIdFromAccessToken(HttpServletRequest request) {
        String statusCd = "5000";
        String statusMsg = "fail";
        AuthUserDto authUserDto = null;
        try {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            String token = authorizationHeader.replace("Bearer ", "");
            if (JwtUtil.isExpired(token)) {
                return new ResponseEntity(HttpStatus.UNAUTHORIZED);
            }
            Long userId = JwtUtil.getId(token);
            statusCd = "2000";
            statusMsg = "success";
            authUserDto = AuthUserDto.builder()
                    .userId(userId)
                    .statusCd(statusCd)
                    .statusMsg(statusMsg)
                    .build();
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            authUserDto = AuthUserDto.builder()
                    .userId(0L)
                    .statusCd(statusCd)
                    .statusMsg(e.getMessage())
                    .build();
            return ResponseEntity.ok(authUserDto);
        }
        return ResponseEntity.ok(authUserDto);
    }
}