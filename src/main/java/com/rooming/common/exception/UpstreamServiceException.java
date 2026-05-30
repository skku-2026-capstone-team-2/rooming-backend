package com.rooming.common.exception;

import org.springframework.http.HttpStatus;

public class UpstreamServiceException extends BusinessException {
    public UpstreamServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }
}