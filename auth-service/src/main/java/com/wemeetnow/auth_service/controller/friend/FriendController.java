package com.wemeetnow.auth_service.controller.friend;

import com.wemeetnow.auth_service.config.jwt.JwtUtil;
import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.ErrorCode;
import com.wemeetnow.auth_service.domain.enums.FriendStatus;
import com.wemeetnow.auth_service.dto.CommonApiResponse;
import com.wemeetnow.auth_service.dto.FriendDeleteRequestDto;
import com.wemeetnow.auth_service.dto.FriendInfoDto;
import com.wemeetnow.auth_service.dto.FriendInvitationAcceptRequestDto;
import com.wemeetnow.auth_service.dto.FriendInvitationRequestDto;
import com.wemeetnow.auth_service.dto.FriendInvitationResponseDto;
import com.wemeetnow.auth_service.dto.FriendStatusUpdateRequestDto;
import com.wemeetnow.auth_service.exception.CustomException;
import com.wemeetnow.auth_service.service.FriendService;
import com.wemeetnow.auth_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/v1/friends")
public class FriendController {

    private final UserService userService;
    private final FriendService friendService;
    private final JwtUtil jwtUtil;

    @Operation(
            summary = "친구 목록 조회",
            description = "Authorization 헤더의 JWT로 로그인 사용자를 확인하고, 해당 사용자의 친구 관계 목록을 조회합니다. " +
                    "삭제된 친구 관계는 결과에서 제외됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않거나 로그인 사용자가 존재하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @CrossOrigin(origins = "https://localhost:3000")
    @GetMapping("/list")
    public ResponseEntity<CommonApiResponse<List<FriendInfoDto>>> getFriendList(HttpServletRequest httpRequest) {
        User loginUser = jwtUtil.getAuthenticatedUser(httpRequest);
        List<FriendInfoDto> friendInfoDtos = friendService.getFriendList(loginUser.getId());
        return ResponseEntity.ok(CommonApiResponse.<List<FriendInfoDto>>builder()
                .statusCode("2000")
                .data(friendInfoDtos)
                .message("친구 목록을 조회했습니다.")
                .build());
    }

    @Operation(
            summary = "친구 초대 수락",
            description = "Authorization 헤더의 JWT로 초대받은 사용자를 확인한 뒤, 지정한 사용자가 보낸 대기 중(WAIT)인 친구 초대를 수락합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "친구 초대 수락 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 본문이 비어 있거나 사용자 ID가 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않거나 로그인 사용자가 존재하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "초대 발신자 또는 수락할 친구 초대가 존재하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @CrossOrigin(origins = "https://localhost:3000")
    @PostMapping("/accept-new-friend")
    public ResponseEntity<CommonApiResponse<Void>> acceptNewFriend(
            @RequestBody(required = false) FriendInvitationAcceptRequestDto request,
            HttpServletRequest httpRequest) {
        User receiver = jwtUtil.getAuthenticatedUser(httpRequest);
        if (request == null || request.getSendUserId() == null || request.getSendUserId() <= 0
                || request.getSendUserId().equals(receiver.getId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User sender = userService.getUserById(request.getSendUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        int updatedCount = friendService.acceptNewFriend(receiver.getId(), sender.getId());
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.FRIEND_INVITATION_NOT_FOUND);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(CommonApiResponse.<Void>builder()
                        .statusCode("2002")
                        .data(null)
                        .message("친구 초대를 수락했습니다.")
                        .build());
    }


    @Operation(
            summary = "친구 초대 보내기",
            description = "Authorization 헤더의 JWT로 로그인 사용자를 확인한 뒤, 요청에 포함된 사용자들에게 친구 초대를 보냅니다. " +
                    "모든 대상 사용자가 존재하고 이미 친구이거나 초대 대기 중인 관계가 없어야 합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 초대 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 본문이 비어 있거나 사용자 ID 목록이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않거나 로그인 사용자가 존재하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "초대 대상 사용자가 존재하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "409", description = "초대 대상 중 이미 친구이거나 초대 대기 중인 사용자가 있음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @CrossOrigin(origins = "https://localhost:3000")
    @PostMapping("/invitations")
    public ResponseEntity<CommonApiResponse<FriendInvitationResponseDto>> sendFriendInvitations(
            @RequestBody(required = false) FriendInvitationRequestDto request,
            HttpServletRequest httpRequest) {
        User sender = jwtUtil.getAuthenticatedUser(httpRequest);
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Long> invitedUserIds = friendService.sendFriendInvitations(
                sender.getId(), request.getReqeustFreiendUserIdList());
        FriendInvitationResponseDto data = new FriendInvitationResponseDto(invitedUserIds.size(), invitedUserIds);
        return ResponseEntity.ok(CommonApiResponse.<FriendInvitationResponseDto>builder()
                .statusCode("2000")
                .data(data)
                .message("친구 초대를 보냈습니다.")
                .build());
    }

    @Operation(
            summary = "친구 상태 변경",
            description = "JWT로 로그인 사용자를 확인한 뒤, 해당 사용자와 대상 사용자 사이의 친구 관계 상태를 변경합니다. " +
                    "상태값은 BLOCK, HIDE, FAVORITE, NEW, REPORTED 중 하나여야 합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "친구 상태 변경 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 데이터 또는 친구 상태값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않거나 로그인 사용자가 존재하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "대상 사용자 또는 친구 관계를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @CrossOrigin(origins = "https://localhost:3000")
    @PostMapping("/update-friend-status")
    public ResponseEntity<CommonApiResponse<Void>> updateFriendStatus(
            @RequestBody(required = false) FriendStatusUpdateRequestDto request,
            HttpServletRequest httpRequest) {
        User loginUser = jwtUtil.getAuthenticatedUser(httpRequest);
        if (request == null || request.getTargetUserId() == null || request.getTargetUserId() <= 0
                || request.getFriendStatus() == null || request.getFriendStatus().isBlank()
                || request.getTargetUserId().equals(loginUser.getId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        FriendStatus newFriendStatus = FriendStatus.getFriendStatusFromStr(request.getFriendStatus());
        if (newFriendStatus == null || newFriendStatus == FriendStatus.WAIT || newFriendStatus == FriendStatus.DEL) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (userService.getUserById(request.getTargetUserId()).isEmpty()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        int updatedCount = friendService.updateFriendStatus(
                newFriendStatus, loginUser.getId(), request.getTargetUserId());
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.FRIEND_RELATIONSHIP_NOT_FOUND);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(CommonApiResponse.<Void>builder()
                        .statusCode("2002")
                        .data(null)
                        .message("친구 상태를 변경했습니다.")
                        .build());
    }

    @Operation(
            summary = "친구 삭제",
            description = "Authorization 헤더의 JWT로 로그인 사용자를 확인한 뒤, 해당 사용자와 대상 사용자 사이의 친구 관계를 삭제 상태로 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 삭제 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 본문 또는 대상 사용자 ID가 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않거나 로그인 사용자가 존재하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "대상 사용자 또는 친구 관계를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @CrossOrigin(origins = "https://localhost:3000")
    @PostMapping("/delete")
    public ResponseEntity<CommonApiResponse<Void>> deleteFriendOne(
            @RequestBody(required = false) FriendDeleteRequestDto request,
            HttpServletRequest httpRequest) {
        User loginUser = jwtUtil.getAuthenticatedUser(httpRequest);
        if (request == null || request.getTargetUserId() == null || request.getTargetUserId() <= 0
                || request.getTargetUserId().equals(loginUser.getId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (userService.getUserById(request.getTargetUserId()).isEmpty()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        int updatedCount = friendService.updateFriendStatus(
                FriendStatus.DEL, loginUser.getId(), request.getTargetUserId());
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.FRIEND_RELATIONSHIP_NOT_FOUND);
        }

        return ResponseEntity.ok(CommonApiResponse.<Void>builder()
                .statusCode("2003")
                .data(null)
                .message("친구를 삭제했습니다.")
                .build());
    }

}
