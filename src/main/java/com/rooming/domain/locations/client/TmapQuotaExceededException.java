package com.rooming.domain.locations.client;

public class TmapQuotaExceededException extends RuntimeException {
    public TmapQuotaExceededException(String message) {
        super(message);
    }
}
