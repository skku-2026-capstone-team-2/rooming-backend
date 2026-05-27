package com.skku.zip.common.exception;

import org.springframework.http.HttpStatus;

public class UpstreamServiceException extends BusinessException {
    public UpstreamServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}
