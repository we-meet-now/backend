package com.wemeetnow.auth_service.controller.friend;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.dto.FriendInfoDto;
import com.wemeetnow.auth_service.service.FriendService;
import com.wemeetnow.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/friends")
public class FriendController {

//    @Value("${front.url}")
//    private final String frontUrl;
    private final UserService userService;
    private final FriendService friendService;

    @CrossOrigin(origins = "https://localhost:3000")
    @GetMapping("/list")
    public ResponseEntity getFriendList(@RequestBody Map<String, Object> reqMap) {
        HttpStatus status = HttpStatus.OK;
        Map<String, Object> body  = new HashMap<>();
        try {
            String accessToken = (String) reqMap.get("accessToken");
            String refreshToken = (String) reqMap.get("refreshToken");
            Long userId = JwtUtil.getId(accessToken);

            List<FriendInfoDto> friendInfoDtos = friendService.getFriendList(userId);
            body.put("data", friendInfoDtos);
            body.put("result", "success");
        } catch (Exception e) {
            status = HttpStatus.BAD_REQUEST;
            log.error("raised error: {}", e.getMessage());
            body.put("data", null);
            body.put("result", "fail");
        }
        return ResponseEntity.status(status).body(body);
    }

    @CrossOrigin(origins = "https://localhost:3000")
    @PostMapping("/accept-new-friend")
    public ResponseEntity acceptNewFriend(@RequestBody Map<String, Object> reqMap) {
        HttpStatus status = HttpStatus.OK;
        Map<String, Object> body  = new HashMap<>();
        try {
            // TODO 내정보는 토큰에 있음 즉, 받는 사람 id(receiveUserId)는 토큰 있음(FE와 어떻게 주고받을지 상의 필요)
//            String accessToken = (String) reqMap.get("accessToken");
//            String refreshToken = (String) reqMap.get("refreshToken");
//            Long receiveUserId = JwtUtil.getId(accessToken);
            Long receiveUserId = 1L;

            Long sendUserId = Long.valueOf((String) reqMap.get("sendUserId")); // 친구추가 보낸 사용자 id
            Optional<User> sendUserOpt = userService.getUserById(sendUserId);// 친구추가 보낸 사용자 정보

            int resultCnt = 0;
            if (sendUserOpt != null && sendUserOpt.isPresent()) {
                // TODO senderId가 존재하는 사용자 인가? 신고당한 사람은 아닌가? 검증하는 로직 필요
                // 친구 생성 로직
                resultCnt = friendService.acceptNewFriend(receiveUserId, sendUserOpt.get().getId());

                log.info("resultCnt: ", resultCnt);
            } else {
                log.info("해당 요청을 보낸사람은 존재하지 않습니다.");
            }

            body.put("data", resultCnt);
            body.put("result", "success");
        } catch (Exception e) {
            status = HttpStatus.BAD_REQUEST;
            log.error("raised error: {}", e.getMessage());
            body.put("data", null);
            body.put("result", "fail");
        }
        return ResponseEntity.status(status).body(body);
    }

    // TODO 친구 초대 보내기 기능 구현예정
    @CrossOrigin(origins = "https://localhost:3000")
    @PostMapping("/send-new-friend")
    public ResponseEntity sendNewFriend(@RequestBody Map<String, Object> reqMap) {
        HttpStatus status = HttpStatus.OK;
        Map<String, Object> body  = new HashMap<>();
        try {

            body.put("data", "");
            body.put("result", "success");
        } catch (Exception e) {
            status = HttpStatus.BAD_REQUEST;
            log.error("raised error: {}", e.getMessage());
            body.put("data", null);
            body.put("result", "fail");
        }
        return ResponseEntity.status(status).body(body);
    }
}
