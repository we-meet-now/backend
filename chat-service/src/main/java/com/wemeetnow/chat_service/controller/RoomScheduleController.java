package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.dto.AuthUserDto;
import com.wemeetnow.chat_service.dto.CommonApiResponse;
import com.wemeetnow.chat_service.dto.RoomScheduleRequestDto;
import com.wemeetnow.chat_service.dto.RoomScheduleResponseDto;
import com.wemeetnow.chat_service.service.CommonService;
import com.wemeetnow.chat_service.service.RoomScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/v1/chat-rooms/{roomId}/schedules")
public class RoomScheduleController {

    private final RoomScheduleService roomScheduleService;
    private final CommonService commonService;

    @Operation(
            summary = "채팅방 일정 목록 조회",
            description = "Authorization 헤더의 JWT로 사용자를 확인하고, 해당 채팅방의 활성 참여자인 경우 채팅방 일정을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 일정 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "인증 실패 또는 서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<CommonApiResponse<List<RoomScheduleResponseDto>>> getSchedules(
            @PathVariable("roomId") Long roomId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        Long userId = getUserId(authorizationHeader);
        List<RoomScheduleResponseDto> schedules = roomScheduleService.getSchedules(roomId, userId);
        return ResponseEntity.ok(CommonApiResponse.<List<RoomScheduleResponseDto>>builder()
                .statusCode(ApiStatusCode.SUCCESS.getCode())
                .data(schedules)
                .message("채팅방 일정 목록 조회 성공")
                .build());
    }

    @Operation(
            summary = "채팅방 일정 상세 조회",
            description = "인증된 채팅방 참여자가 일정 ID로 해당 채팅방의 활성 일정을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 일정 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "채팅방 일정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/{scheduleId}")
    public ResponseEntity<CommonApiResponse<RoomScheduleResponseDto>> getSchedule(
            @PathVariable("roomId") Long roomId,
            @PathVariable("scheduleId") Long scheduleId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        Long userId = getUserId(authorizationHeader);
        RoomScheduleResponseDto schedule = roomScheduleService.getSchedule(roomId, scheduleId, userId);
        return ResponseEntity.ok(CommonApiResponse.<RoomScheduleResponseDto>builder()
                .statusCode(ApiStatusCode.SUCCESS.getCode())
                .data(schedule)
                .message("채팅방 일정 조회 성공")
                .build());
    }

    @Operation(
            summary = "채팅방 일정 생성",
            description = "Authorization 헤더의 JWT로 인증하고 roomId를 경로에서 받습니다. " +
                    "요청 본문은 planYmd, planHm, planAmpm, storeId, meetingType이며 userId는 토큰에서 가져오고 useYn은 Y로 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "채팅방 일정 생성 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 데이터가 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CommonApiResponse<RoomScheduleResponseDto>> createSchedule(
            @PathVariable("roomId") Long roomId,
            @RequestBody RoomScheduleRequestDto request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        Long userId = getUserId(authorizationHeader);
        RoomScheduleResponseDto schedule = roomScheduleService.createSchedule(roomId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonApiResponse.<RoomScheduleResponseDto>builder()
                        .statusCode(ApiStatusCode.CREATED.getCode())
                        .data(schedule)
                        .message("채팅방 일정 생성 성공")
                        .build());
    }

    @Operation(
            summary = "채팅방 일정 수정",
            description = "Authorization 헤더의 JWT로 인증하고 planYmd, planHm, planAmpm, storeId, meetingType을 수정합니다. " +
                    "활성 참여자만 가능하며 작성자와 채팅방 ID는 변경할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 일정 수정 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 데이터가 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "채팅방 일정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PutMapping("/{scheduleId}")
    public ResponseEntity<CommonApiResponse<RoomScheduleResponseDto>> updateSchedule(
            @PathVariable("roomId") Long roomId,
            @PathVariable("scheduleId") Long scheduleId,
            @RequestBody RoomScheduleRequestDto request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        Long userId = getUserId(authorizationHeader);
        RoomScheduleResponseDto schedule = roomScheduleService.updateSchedule(
                roomId, scheduleId, userId, request);
        return ResponseEntity.ok(CommonApiResponse.<RoomScheduleResponseDto>builder()
                .statusCode(ApiStatusCode.UPDATED.getCode())
                .data(schedule)
                .message("채팅방 일정 수정 성공")
                .build());
    }

    @Operation(
            summary = "채팅방 일정 삭제",
            description = "인증된 채팅방 참여자가 해당 채팅방의 활성 일정을 useYn='N'으로 비활성 처리합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 일정 삭제 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "채팅방 일정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<CommonApiResponse<Void>> deleteSchedule(
            @PathVariable("roomId") Long roomId,
            @PathVariable("scheduleId") Long scheduleId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        Long userId = getUserId(authorizationHeader);
        roomScheduleService.deleteSchedule(roomId, scheduleId, userId);
        return ResponseEntity.ok(CommonApiResponse.<Void>builder()
                .statusCode(ApiStatusCode.DELETED.getCode())
                .data(null)
                .message("채팅방 일정 삭제 성공")
                .build());
    }

    private Long getUserId(String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        AuthUserDto authUser = commonService.fetchUserFromAuthService(token);
        return authUser.getUserId();
    }
}
