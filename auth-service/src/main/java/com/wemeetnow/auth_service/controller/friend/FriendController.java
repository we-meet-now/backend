package com.wemeetnow.auth_service.controller.friend;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.dto.FriendInfoDto;
import com.wemeetnow.auth_service.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/friends")
public class FriendController {

    @Value("${front.url}")
    private final String frontUrl;

    private FriendService friendService;

    @CrossOrigin(origins = "https://localhost:3000")
    @GetMapping("/list")
    public ResponseEntity getFriendList(@RequestBody Map<String, Object> reqMap) {
        String accessToken = (String) reqMap.get("accessToken");
        String refreshToken = (String) reqMap.get("refreshToken");
        Long userId = JwtUtil.getId(accessToken);

        List<FriendInfoDto> friendInfoDtos = friendService.getFriendList(userId);

    }
}
