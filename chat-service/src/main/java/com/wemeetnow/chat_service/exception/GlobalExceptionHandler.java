package com.wemeetnow.chat_service.exception;

import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.dto.CommonApiResponse;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // springdoc(Swagger) 내부 동작 중 발생하는 ServletException은 처리하지 않음 (재throw)
    @ExceptionHandler(ServletException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleServletException(ServletException ex) throws ServletException {
        throw ex;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleInvalidRequestBody(HttpMessageNotReadableException ex) {
        CommonApiResponse<Void> response = CommonApiResponse.<Void>builder()
                .statusCode(ApiStatusCode.BAD_REQUEST.getCode())
                .data(null)
                .message("요청 본문 형식이 올바르지 않습니다.")
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /** 400 - 일정 시간 역전 등 잘못된 요청 */
    @ExceptionHandler(InvalidScheduleTimeException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleInvalidScheduleTime(InvalidScheduleTimeException ex) {
        log.warn("InvalidScheduleTimeException: {}", ex.getMessage());
        CommonApiResponse<Void> response = CommonApiResponse.<Void>builder()
                .statusCode(ApiStatusCode.BAD_REQUEST.getCode())
                .data(null)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /** 403 - 일정 수정/삭제 권한 없음 */
    @ExceptionHandler(ScheduleAccessDeniedException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleScheduleAccessDenied(ScheduleAccessDeniedException ex) {
        log.warn("ScheduleAccessDeniedException: {}", ex.getMessage());
        CommonApiResponse<Void> response = CommonApiResponse.<Void>builder()
                .statusCode(ApiStatusCode.SCHEDULE_ACCESS_DENIED.getCode())
                .data(null)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /** 404 - 존재하지 않는 일정 */
    @ExceptionHandler(ScheduleNotFoundException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleScheduleNotFound(ScheduleNotFoundException ex) {
        log.warn("ScheduleNotFoundException: {}", ex.getMessage());
        CommonApiResponse<Void> response = CommonApiResponse.<Void>builder()
                .statusCode(ApiStatusCode.SCHEDULE_NOT_FOUND.getCode())
                .data(null)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ChatRoomInviteException.class)
    public ResponseEntity<CommonApiResponse<Object>> handleChatRoomInviteException(ChatRoomInviteException ex) {
        log.warn("Chat room invite rejected: {}", ex.getMessage());
        CommonApiResponse<Object> response = CommonApiResponse.builder()
                .statusCode(ex.getApiStatusCode().getCode())
                .data(ex.getData())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(ChatRoomParticipantException.class)
    public ResponseEntity<CommonApiResponse<Object>> handleChatRoomParticipantException(
            ChatRoomParticipantException ex) {
        log.warn("Chat room participant operation rejected: {}", ex.getMessage());
        CommonApiResponse<Object> response = CommonApiResponse.builder()
                .statusCode(ex.getApiStatusCode().getCode())
                .data(ex.getData())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        CommonApiResponse<Void> response = CommonApiResponse.<Void>builder()
                .statusCode(ApiStatusCode.INTERNAL_SERVER_ERROR.getCode())
                .data(null)
                .message(ex.getMessage() != null ? ex.getMessage() : "서버 오류가 발생했습니다.")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
