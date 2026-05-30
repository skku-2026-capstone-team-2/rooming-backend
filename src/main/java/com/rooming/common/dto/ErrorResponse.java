package com.rooming.common.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        String message,
        LocalDateTime time
) {
    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, LocalDateTime.now());
    }
}