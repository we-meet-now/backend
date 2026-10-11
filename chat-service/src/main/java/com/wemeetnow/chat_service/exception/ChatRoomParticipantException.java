package com.wemeetnow.chat_service.exception;

import com.wemeetnow.chat_service.constant.ApiStatusCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ChatRoomParticipantException extends RuntimeException {
    private final ApiStatusCode apiStatusCode;
    private final HttpStatus httpStatus;
    private final Object data;

    public ChatRoomParticipantException(ApiStatusCode apiStatusCode, HttpStatus httpStatus, String message) {
        this(apiStatusCode, httpStatus, message, null);
    }

    public ChatRoomParticipantException(
            ApiStatusCode apiStatusCode, HttpStatus httpStatus, String message, Object data) {
        super(message);
        this.apiStatusCode = apiStatusCode;
        this.httpStatus = httpStatus;
        this.data = data;
    }
}
