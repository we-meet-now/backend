package com.wemeetnow.chat_service.exception;

public class ScheduleAccessDeniedException extends RuntimeException {
    public ScheduleAccessDeniedException(String message) {
        super(message);
    }
}

