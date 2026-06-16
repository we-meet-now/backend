package com.wemeetnow.chat_service.exception;

public class InvalidScheduleTimeException extends RuntimeException {
    public InvalidScheduleTimeException(String message) {
        super(message);
    }
}

