# 📅 개인 일정 관리 (User Schedule) API 컨트롤러 설계서

본 문서는 프로젝트 내 채팅 서비스 컨트롤러의 구현 스타일(Swagger 어노테이션, 공통 응답 포맷, 토큰 인증 로직)을 반영한 개인 일정 관리 API 설계서입니다.

---

## 0. 참고할 엔티티(domain폴더에 존재함)
- `UserSchedule`: 개인 일정 엔티티 (일정 ID, 제목, 시간, 공개 여부, 생성자 ID 등)
- `UserScheduleParticipant`: 일정 참여자 매핑 엔티티 (일정 ID, 참여

## 📌 1. 공통 가이드라인 (Coding Convention)

* **인증 방식 및 유저 식별:** 헤더의 `Authorization` 토큰(`Bearer {Token}`)을 검증하여 요청자(`loginedUserId`)를 식별합니다.
* **공통 응답 포맷:** 모든 API는 `ResponseEntity<CommonApiResponse<T>>` 구조로 반환합니다.
* **성공 상태 코드 (statusCode):** 일정 조회: `2000` / 일정 생성: `2001` / 일정 수정: `2002` / 일정 삭제: `2003` (프로젝트 규칙에 맞게 확장 가능)

---

## 🚀 2. API 컨트롤러 코드 설계 (Controller Specification)

### 2.1 개인 일정 생성 (POST)
> 채팅방 생성(`create-one`) 스타일을 참고하여, 요청 본문(RequestBody)으로 일정 정보를 받고 토큰을 통해 생성자를 식별하여 참여자 매핑과 함께 일정을 생성합니다.

```java
@Operation(
        summary = "개인 일정 생성 (다중 참여자 초대)",
        description = "일정 정보(제목, 시간 등)와 참여자 ID 목록을 받아 일정을 생성하고, " +
                      "요청한 사용자 및 초대된 참여자들을 일정 참여자로 등록합니다. " +
                      "Authorization 헤더의 JWT 토큰으로 요청자를 식별합니다."
)
@ApiResponses({
        @ApiResponse(responseCode = "201", description = "일정 생성 성공",
                content = @Content(schema = @Schema(implementation = CreateScheduleResponseDto.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류 (토큰 인증 실패, DB 저장 실패 등)",
                content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
})
@PostMapping("")
public ResponseEntity<CommonApiResponse<CreateScheduleResponseDto>> createSchedule(
        @RequestBody CreateScheduleRequestDto createScheduleRequestDto,
        HttpServletRequest request) {
    
    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    String accessToken = authorizationHeader.replace("Bearer ", "");
    
    // Auth 서비스 연동을 통한 유저 정보 조회
    AuthUserDto authUserDto = scheduleService.fetchUserFromAuthService(accessToken);
    Long creatorUserId = authUserDto.getUserId();

    // 서비스 레이어 호출 (일정 생성 및 참여자 매핑)
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
```

---

### 2.2 일정 내역 및 참여자 조회 (GET)
> 채팅방 입장(`roomId={roomId}`) 스타일을 참고하여, 일정 ID로 해당 일정의 상세 내용과 참여자 목록을 함께 조회합니다. 로그인한 유저 ID 기반으로 비공개 여부 필터링을 수행합니다.

```java
@Operation(
        summary = "일정 상세 조회 (참여자 내역 포함)",
        description = "일정 ID로 해당 일정의 상세 정보와 참여자 목록을 조회합니다. " +
                      "로그인한 사용자 ID를 바탕으로 공개 여부(Y/N)에 따른 데이터 마스킹을 처리합니다."
)
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "일정 상세 조회 성공",
                content = @Content(schema = @Schema(implementation = ScheduleDetailResponseDto.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류 (토큰 인증 실패, 잘못된 scheduleId 등)",
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

    // 서비스 레이어에서 로그인 유저의 권한 및 공개 여부 필터링 처리 후 DTO 반환
    ScheduleDetailResponseDto responseDto = scheduleService.getScheduleDetail(scheduleId, loginedUserId);

    return ResponseEntity.ok(
            CommonApiResponse.<ScheduleDetailResponseDto>builder()
                    .statusCode("2000")
                    .data(responseDto)
                    .message("일정 상세 조회 성공")
                    .build()
    );
}
```

---

### 2.3 개인 일정 전체 수정 (PUT) 및 상태 변경 (PATCH)
> 비즈니스 목적에 맞춰 일정 데이터를 전체 수정하거나, `status`만 원자적으로 변경(미정->확정/취소)할 수 있는 엔드포인트를 제공합니다.

```java
@Operation(
        summary = "개인 일정 수정",
        description = "일정의 제목, 시간, 공개 여부 등을 전체 수정합니다. 일정 소유자만 수정이 가능합니다."
)
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

@Operation(
        summary = "일정 상태 변경",
        description = "일정의 상태(PENDING, CONFIRMED, CANCELLED)만 부분적으로 변경합니다."
)
@PatchMapping("/scheduleId={scheduleId}/status")
public ResponseEntity<CommonApiResponse<UpdateScheduleStatusResponseDto>> updateScheduleStatus(
        @PathVariable("scheduleId") Long scheduleId,
        @RequestBody UpdateStatusRequestDto statusRequestDto,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    
    String accessToken = token.replace("Bearer ", "");
    AuthUserDto authUserDto = scheduleService.fetchUserFromAuthService(accessToken);
    Long loginedUserId = authUserDto.getUserId();

    UpdateScheduleStatusResponseDto responseDto = scheduleService.updateScheduleStatus(scheduleId, loginedUserId, statusRequestDto.getStatus());

    return ResponseEntity.ok(
            CommonApiResponse.<UpdateScheduleStatusResponseDto>builder()
                    .statusCode("2002")
                    .data(responseDto)
                    .message("일정 상태 변경 성공")
                    .build()
    );
}
```

---

### 2.4 개인 일정 삭제 (DELETE)

```java
@Operation(
        summary = "개인 일정 삭제",
        description = "해당 일정을 삭제합니다. 일정 소유자 권한 확인이 수행됩니다."
)
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
```

---

### 🔒 4. 예외 처리 및 예방 벨리데이션 (Validation & Exceptions)

1. **시간 역전 방지:** `startAt`이 `endAt`보다 미래의 시간일 수 없습니다. (400 Bad Request)

2. **권한 검증:** 수정(PUT, PATCH) 및 삭제(DELETE) 요청 시, 요청자(requesterId)가 해당 일정의 소유자 혹은 등록 권한이 있는지 검증해야 합니다. (403 Forbidden)

3. **존재하지 않는 일정:** 존재하지 않는 `scheduleId`로 요청이 올 경우 에러를 반환합니다. (404 Not Found)
   (`com.wemeetnow.chat_service.exception.GlobalExceptionHandler` 참고해서 전역 예외처리 구현/활용)

4. **statusCode**는 성공 응답은 `"2000"`, 클라이언트 오류는 `"4000"`대, 서버 오류는 `"5000"`대로 구분하여 일관된 API 응답 구조를 유지합니다. (`dto/CommonApiResponse` 활용)
```