package com.rooming.domain.locations.dto;

public record PropertyInfrastructureSyncResult(
        int infrastructureCount,
        int createdAccessibilityCount,
        int removedAccessibilityCount,
        boolean quotaExceeded
) {
}
