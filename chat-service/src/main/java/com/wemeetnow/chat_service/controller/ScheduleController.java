package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.dto.*;
import com.wemeetnow.chat_service.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "개인 일정 API", description = "개인 일정 생성, 조회, 수정, 상태 변경, 삭제 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(
            summary = "내 일정 월별 전체 조회",
            description = "Authorization 헤더의 JWT 액세스 토큰으로 요청자를 인증하고, 요청자가 생성한 일정을 조회합니다. " +
                    "month 쿼리 파라미터는 선택값이며 yyyyMM 형식입니다. 생략하면 현재 월을 조회하고, " +
                    "일정 시작일(startAt) 기준으로 해당 월을 필터링해 시작일 오름차순으로 반환합니다. " +
                    "응답의 시작일과 종료일은 yyyyMMdd 문자열입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일정 목록 조회 성공",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = ScheduleListItemResponseDto.class)))),
            @ApiResponse(responseCode = "400", description = "month 형식이 yyyyMM이 아니거나 유효하지 않은 월",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "인증 실패 또는 서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("")
    public ResponseEntity<CommonApiResponse<List<ScheduleListItemResponseDto>>> getSchedules(
            @RequestParam(required = false) String month,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {

        String accessToken = token.replace("Bearer ", "");
        AuthUserDto authUserDto = scheduleService.fetchUserFromAuthService(accessToken);
        List<ScheduleListItemResponseDto> responseDto =
                scheduleService.getSchedules(authUserDto.getUserId(), month);

        return ResponseEntity.ok(
                CommonApiResponse.<List<ScheduleListItemResponseDto>>builder()
                        .statusCode(ApiStatusCode.SUCCESS.getCode())
                        .data(responseDto)
                        .message("일정 목록 조회 성공")
                        .build()
        );
    }

    @Operation(
            summary = "친구의 공개 일정 월별 조회",
            description = "JWT로 요청자를 인증하고 Auth Service의 친구 관계를 확인한 뒤, 친구가 공개한 일정만 조회합니다. " +
                    "친구 상태가 NEW, FAVORITE, HIDE인 경우에만 허용되며 month는 선택 yyyyMM 형식입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 공개 일정 조회 성공",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = ScheduleListItemResponseDto.class)))),
            @ApiResponse(responseCode = "400", description = "month 형식이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "403", description = "친구가 아니거나 공개 일정 조회 권한이 없음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/friends/{userId}")
    public ResponseEntity<CommonApiResponse<List<ScheduleListItemResponseDto>>> getFriendSchedules(
            @PathVariable("userId") Long userId,
            @RequestParam(required = false) String month,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        String accessToken = token.replace("Bearer ", "");
        AuthUserDto authUserDto = scheduleService.fetchUserFromAuthService(accessToken);
        List<ScheduleListItemResponseDto> schedules = scheduleService.getFriendSchedules(
                userId, authUserDto.getUserId(), token, month);

        return ResponseEntity.ok(CommonApiResponse.<List<ScheduleListItemResponseDto>>builder()
                .statusCode(ApiStatusCode.SUCCESS.getCode())
                .data(schedules)
                .message("친구 공개 일정 조회 성공")
                .build());
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/chat/v1/schedules  →  일정 생성
    // ─────────────────────────────────────────────────────────

    @Operation(
            summary = "개인 일정 생성 (다중 참여자 초대)",
            description = "일정 정보(제목, 시간 등)와 참여자 ID 목록을 받아 일정을 생성하고, " +
                          "요청한 사용자 및 초대된 참여자들을 일정 참여자로 등록합니다. " +
                          "publicScheduleYn과 publicMemberYn은 Y/N만 허용하며, 생략하면 비공개(N)로 저장합니다. " +
                          "Authorization 헤더의 JWT 토큰으로 요청자를 식별합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "일정 생성 성공",
                    content = @Content(schema = @Schema(implementation = CreateScheduleResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 시간 또는 공개 여부 입력",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (토큰 인증 실패, DB 저장 실패 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping("")
    public ResponseEntity<CommonApiResponse<CreateScheduleResponseDto>> createSchedule(
            @RequestBody CreateScheduleRequestDto createScheduleRequestDto,
            HttpServletRequest request) {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = authorizationHeader.replace("Bearer ", "");

        AuthUserDto authUserDto = scheduleService.fetchUserFromAuthService(accessToken);
        Long creatorUserId = authUserDto.getUserId();

        Long userScheduleId = scheduleService.createSchedule(creatorUserId, createScheduleRequestDto);

        CreateScheduleResponseDto responseDto = new CreateScheduleResponseDto(userScheduleId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonApiResponse.<CreateScheduleResponseDto>builder()
                        .statusCode(ApiStatusCode.CREATED.getCode())
                        .data(responseDto)
                        .message("일정 생성 성공")
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/chat/v1/schedules/scheduleId={scheduleId}  →  일정 상세 조회
    // ─────────────────────────────────────────────────────────

    @Operation(
            summary = "일정 상세 조회 (참여자 내역 포함)",
            description = "일정 ID로 해당 일정의 상세 정보와 참여자 목록을 조회합니다. " +
                          "본인 또는 일정 참여자는 조회할 수 있으며, 비참여자는 친구 상태가 NEW, FAVORITE, HIDE인 일정 소유자의 공개 일정만 조회할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일정 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ScheduleDetailResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "일정 공개 범위 또는 친구 권한 없음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 일정",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (토큰 인증 실패 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/scheduleId={scheduleId}")
    public ResponseEntity<CommonApiResponse<ScheduleDetailResponseDto>> getScheduleDetail(
            @PathVariable("scheduleId") Long scheduleId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {

        log.info("scheduleId: {}", scheduleId);

        String accessToken = token.replace("Bearer ", "");
        AuthUserDto authUserDto = scheduleService.fetchUserFromAuthService(accessToken);
        Long loginedUserId = authUserDto.getUserId();

        ScheduleDetailResponseDto responseDto = scheduleService.getScheduleDetail(
                scheduleId, loginedUserId, token);

        return ResponseEntity.ok(
                CommonApiResponse.<ScheduleDetailResponseDto>builder()
                        .statusCode(ApiStatusCode.SUCCESS.getCode())
                        .data(responseDto)
                        .message("일정 상세 조회 성공")
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────
    // PUT /api/chat/v1/schedules/scheduleId={scheduleId}  →  일정 전체 수정
    // ─────────────────────────────────────────────────────────

    @Operation(
            summary = "개인 일정 수정",
            description = "일정의 제목, 시간, 공개 여부 등을 수정합니다. 공개 여부는 Y/N만 허용하며, " +
                    "생략하면 기존 값을 유지합니다. 일정 소유자만 수정이 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일정 수정 성공",
                    content = @Content(schema = @Schema(implementation = UpdateScheduleResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 시간 또는 공개 여부 입력",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 일정",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PutMapping("/scheduleId={scheduleId}")
    public ResponseEntity<CommonApiResponse<UpdateScheduleResponseDto>> updateSchedule(
            @PathVariable("scheduleId") Long scheduleId,
            @RequestBody UpdateScheduleRequestDto updateScheduleRequestDto,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {

        String accessToken = token.replace("Bearer ", "");
        AuthUserDto authUserDto = scheduleService.fetchUserFromAuthService(accessToken);
        Long loginedUserId = authUserDto.getUserId();

        UpdateScheduleResponseDto responseDto = scheduleService.updateSchedule(scheduleId, loginedUserId, updateScheduleRequestDto);

        return ResponseEntity.ok(
                CommonApiResponse.<UpdateScheduleResponseDto>builder()
                        .statusCode(ApiStatusCode.UPDATED.getCode())
                        .data(responseDto)
                        .message("일정 수정 성공")
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────
    // PATCH /api/chat/v1/schedules/scheduleId={scheduleId}/status  →  상태 변경
    // ─────────────────────────────────────────────────────────

    @Operation(
            summary = "일정 상태 변경",
            description = "일정의 상태(PENDING, CONFIRMED, CANCELLED)만 부분적으로 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일정 상태 변경 성공",
                    content = @Content(schema = @Schema(implementation = UpdateScheduleStatusResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "변경 권한 없음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 일정",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PatchMapping("/scheduleId={scheduleId}/status")
    public ResponseEntity<CommonApiResponse<UpdateScheduleStatusResponseDto>> updateScheduleStatus(
            @PathVariable("scheduleId") Long scheduleId,
            @RequestBody UpdateStatusRequestDto statusRequestDto,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {

        String accessToken = token.replace("Bearer ", "");
        AuthUserDto authUserDto = scheduleService.fetchUserFromAuthService(accessToken);
        Long loginedUserId = authUserDto.getUserId();

        UpdateScheduleStatusResponseDto responseDto = scheduleService.updateScheduleStatus(
                scheduleId, loginedUserId, statusRequestDto.getStatus());

        return ResponseEntity.ok(
                CommonApiResponse.<UpdateScheduleStatusResponseDto>builder()
                        .statusCode(ApiStatusCode.UPDATED.getCode())
                        .data(responseDto)
                        .message("일정 상태 변경 성공")
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────
    // DELETE /api/chat/v1/schedules/scheduleId={scheduleId}  →  일정 삭제
    // ─────────────────────────────────────────────────────────

    @Operation(
            summary = "개인 일정 삭제",
            description = "해당 일정을 삭제합니다. 일정 소유자 권한 확인이 수행됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일정 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 일정",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @DeleteMapping("/scheduleId={scheduleId}")
    public ResponseEntity<CommonApiResponse<Void>> deleteSchedule(
            @PathVariable("scheduleId") Long scheduleId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {

        String accessToken = token.replace("Bearer ", "");
        AuthUserDto authUserDto = scheduleService.fetchUserFromAuthService(accessToken);
        Long loginedUserId = authUserDto.getUserId();

        scheduleService.deleteSchedule(scheduleId, loginedUserId);

        return ResponseEntity.ok(
                CommonApiResponse.<Void>builder()
                        .statusCode(ApiStatusCode.DELETED.getCode())
                        .message("일정 삭제 성공")
                        .build()
        );
    }
}
