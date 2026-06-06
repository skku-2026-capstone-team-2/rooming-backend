package com.rooming.domain.locations.dto;

public record InfrastructureMaintenanceRepairResult(
        int processedPropertyCount,
        int deletedInvalidAccessibilityCount,
        int nearbyInfrastructureFetchedCount,
        int infraAccessibilityFetchedCount
) {
}
