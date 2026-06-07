package com.rooming.domain.locations.dto;

public record PropertyInfrastructureSyncResult(
        int infrastructureCount,
        int createdAccessibilityCount,
        int removedAccessibilityCount,
        boolean poiQuotaExceeded,
        boolean walkingRouteQuotaExceeded
) {
    public boolean quotaExceeded() {
        return poiQuotaExceeded || walkingRouteQuotaExceeded;
    }
}
