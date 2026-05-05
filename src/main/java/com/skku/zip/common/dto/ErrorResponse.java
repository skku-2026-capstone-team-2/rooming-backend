package com.skku.zip.common.dto;

public record ErrorResponse(
        boolean success,
        String errorCode,
        String message
) {
    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(false, errorCode, message);
    }
}
