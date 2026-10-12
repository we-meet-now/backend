package com.wemeetnow.auth_service.constant;

public enum ApiStatusCode {
    SUCCESS("2000"),
    CREATED("2001"),
    UPDATED("2002"),
    DELETED("2003"),
    ACCEPTED("2020"),
    GUEST_TOKEN_ISSUED("2030"),
    BAD_REQUEST("4000"),
    SCHEDULE_ACCESS_DENIED("4003"),
    SCHEDULE_NOT_FOUND("4004"),
    UNAUTHORIZED("4010"),
    CHAT_ROOM_ACCESS_DENIED("4030"),
    RESOURCE_NOT_FOUND("4040"),
    CONFLICT("4090"),
    SERVER_ERROR("5000"),
    INTERNAL_SERVER_ERROR("5005");

    private final String code;

    ApiStatusCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

