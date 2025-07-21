package com.wemeetnow.auth_service.controller.friend;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.FriendStatus;
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

    // 친구목록 조회 API
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
            FriendStatus friendStatus = FriendStatus.NEW;
            if (sendUserOpt != null && sendUserOpt.isPresent()) {
                // TODO senderId가 존재하는 사용자 인가? 신고당한 사람은 아닌가? 검증하는 로직 필요
                // 친구 생성 로직
                log.info("sendUserOpt.get().getId(): [{}]", sendUserOpt.get().getId());// 2
                log.info("friendStatus.getStatus(): [{}]", friendStatus.getStatus());
                resultCnt = friendService.acceptNewFriend(receiveUserId, sendUserOpt.get().getId(), friendStatus);

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


    @CrossOrigin(origins = "https://localhost:3000")
    @PostMapping("/send-new-friend")
    public ResponseEntity sendNewFriend(@RequestBody Map<String, Object> reqMap) {
        HttpStatus status = HttpStatus.OK;
        Map<String, Object> body  = new HashMap<>();
        try {
            Long sendUserId = 2L;

            Long receiveUserId = Long.valueOf((String) reqMap.get("receiveUserId")); // 친구초대 받을 사용자 id
            Optional<User> receiveUserOpt = userService.getUserById(receiveUserId);// 친구추가 보낸 사용자 정보

            int resultCnt = 0;
            FriendStatus friendStatus = FriendStatus.WAIT;
            if (receiveUserOpt != null && receiveUserOpt.isPresent()) {
                // TODO receiveUserId 가 존재하는 사용자 인가? 신고당한 사람은 아닌가? 검증하는 로직 필요
                // 친구 생성 로직
                resultCnt = friendService.sendNewFriend(sendUserId, receiveUserOpt.get().getId(), friendStatus); // CHECK 왜 REPORTED 값이 들어갈까??
                System.out.println("friendStatus = " + friendStatus);
                body.put("message", "success");

                log.info("resultCnt: {}", resultCnt);
            } else {
                log.info("친구 초대 받을 사용자가 존재하지 않습니다.");
                body.put("message", "친구 초대 받을 사용자가 존재하지 않습니다.");
            }

            body.put("data", "1");
            body.put("result", "success");
        } catch (Exception e) {
            status = HttpStatus.BAD_REQUEST;
            log.error("raised error: {}", e.getMessage());
            body.put("data", null);
            body.put("result", "fail");
        }
        return ResponseEntity.status(status).body(body);
    }

    // TODO 친구상태 변경 API
    @CrossOrigin(origins = "https://localhost:3000")
    @PostMapping("/update-friend-status")
    public ResponseEntity updateFriendStatus(@RequestBody Map<String, Object> reqMap) {
        HttpStatus status = HttpStatus.OK;
        Map<String, Object> body  = new HashMap<>();
        int resultCnt = 0;
        try {
            Long loginUserId = 2L; // TODO token에서 로그인한 사용자id 추출해야됨

            Long targetUserId = Long.valueOf((String) reqMap.get("targetUserId")); // 대상이 되는 사용자 id
            // 다음 상태값을 기준으로 reqMap의 type값에 따라 친구상태 값 변경하기 (BLOCK", "차단"), HIDE("HIDE", "숨김"), FAVORITE("FAVORITE", "즐겨찾기"), NEW("NEW", "신규"), REPORTED("REPORTED", "신고")
            // 나중에 NEW 친구의 상태값을 노말 상태로 바꿀 수 있음
            if (reqMap.get("friendStatus") == null || reqMap.get("friendStatus").equals("")) {
                body.put("message", "변경할 상태값을 선택해주세요.");
                status = HttpStatus.NOT_ACCEPTABLE;
            } else {
                resultCnt = friendService.updateFriendStatus((String) reqMap.get("friendStatus"), loginUserId, targetUserId);
                body.put("message", "success");
                status = HttpStatus.ACCEPTED;
            }
            log.info("resultCnt: {}", resultCnt);

        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
        }
        return ResponseEntity.status(status).body(body);
    }
}
