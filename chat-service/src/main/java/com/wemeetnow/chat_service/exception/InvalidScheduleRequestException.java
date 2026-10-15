package com.wemeetnow.chat_service.exception;

public class InvalidScheduleRequestException extends RuntimeException {
    public InvalidScheduleRequestException(String message) {
        super(message);
    }
}
