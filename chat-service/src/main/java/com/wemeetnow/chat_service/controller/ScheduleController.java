package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.dto.*;
import com.wemeetnow.chat_service.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "개인 일정 API", description = "개인 일정 생성, 조회, 수정, 상태 변경, 삭제 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    // ─────────────────────────────────────────────────────────
    // POST /api/v1/schedules  →  일정 생성
    // ─────────────────────────────────────────────────────────

    @Operation(
            summary = "개인 일정 생성 (다중 참여자 초대)",
            description = "일정 정보(제목, 시간 등)와 참여자 ID 목록을 받아 일정을 생성하고, " +
                          "요청한 사용자 및 초대된 참여자들을 일정 참여자로 등록합니다. " +
                          "Authorization 헤더의 JWT 토큰으로 요청자를 식별합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "일정 생성 성공",
                    content = @Content(schema = @Schema(implementation = CreateScheduleResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 시간 입력 (startAt > endAt)",
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
                        .statusCode("2001")
                        .data(responseDto)
                        .message("일정 생성 성공")
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/schedules/scheduleId={scheduleId}  →  일정 상세 조회
    // ─────────────────────────────────────────────────────────

    @Operation(
            summary = "일정 상세 조회 (참여자 내역 포함)",
            description = "일정 ID로 해당 일정의 상세 정보와 참여자 목록을 조회합니다. " +
                          "로그인한 사용자 ID를 바탕으로 공개 여부(Y/N)에 따른 데이터 마스킹을 처리합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일정 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ScheduleDetailResponseDto.class))),
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

        ScheduleDetailResponseDto responseDto = scheduleService.getScheduleDetail(scheduleId, loginedUserId);

        return ResponseEntity.ok(
                CommonApiResponse.<ScheduleDetailResponseDto>builder()
                        .statusCode("2000")
                        .data(responseDto)
                        .message("일정 상세 조회 성공")
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────
    // PUT /api/v1/schedules/scheduleId={scheduleId}  →  일정 전체 수정
    // ─────────────────────────────────────────────────────────

    @Operation(
            summary = "개인 일정 수정",
            description = "일정의 제목, 시간, 공개 여부 등을 전체 수정합니다. 일정 소유자만 수정이 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일정 수정 성공",
                    content = @Content(schema = @Schema(implementation = UpdateScheduleResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 시간 입력",
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
                        .statusCode("2002")
                        .data(responseDto)
                        .message("일정 수정 성공")
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────
    // PATCH /api/v1/schedules/scheduleId={scheduleId}/status  →  상태 변경
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
                        .statusCode("2002")
                        .data(responseDto)
                        .message("일정 상태 변경 성공")
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────
    // DELETE /api/v1/schedules/scheduleId={scheduleId}  →  일정 삭제
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
                        .statusCode("2003")
                        .message("일정 삭제 성공")
                        .build()
        );
    }
}


