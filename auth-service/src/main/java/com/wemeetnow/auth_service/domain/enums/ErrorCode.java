package com.wemeetnow.auth_service.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "4000", "유효하지 않은 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "5000", "서버 내부 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "4010", "요청이 잘못됐습니다."),
    // 게스트 인증 관련 에러
    GUEST_USER_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "5051", "게스트 토큰 생성 중 사용자 생성에 실패했습니다."),
    GUEST_TOKEN_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "5050", "게스트 토큰 생성에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
