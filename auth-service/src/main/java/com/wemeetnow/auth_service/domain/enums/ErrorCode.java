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
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "4010", "유효한 사용자 인증이 필요합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "4040", "요청한 사용자를 찾을 수 없습니다."),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "4090", "이미 사용 중인 username입니다."),
    FRIEND_RELATIONSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "4041", "친구 관계를 찾을 수 없습니다."),
    FRIEND_INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "4042", "수락할 친구 초대를 찾을 수 없습니다."),
    FRIEND_ALREADY_EXISTS(HttpStatus.CONFLICT, "4090", "이미 친구이거나 친구 초대가 존재하는 사용자입니다."),
    // 게스트 인증 관련 에러
    GUEST_USER_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "5051", "게스트 토큰 생성 중 사용자 생성에 실패했습니다."),
    GUEST_TOKEN_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "5050", "게스트 토큰 생성에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
