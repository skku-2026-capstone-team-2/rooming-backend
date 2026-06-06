package com.rooming.domain.locations.controller;

import com.rooming.common.dto.ApiResponse;
import com.rooming.domain.locations.dto.InfrastructureMaintenanceRepairResult;
import com.rooming.domain.locations.service.PropertyInfrastructureService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/maintenance/infrastructure-sync-repair-20260606-9f4a2c7d")
public class InfrastructureMaintenanceController {

    private final PropertyInfrastructureService propertyInfrastructureService;

    @GetMapping
    public ResponseEntity<ApiResponse<InfrastructureMaintenanceRepairResult>> repairByGet() {
        return repair();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InfrastructureMaintenanceRepairResult>> repairByPost() {
        return repair();
    }

    private ResponseEntity<ApiResponse<InfrastructureMaintenanceRepairResult>> repair() {
        return ResponseEntity.ok(ApiResponse.success(
                propertyInfrastructureService.repairInfrastructureSyncState(),
                "Infrastructure sync state repaired."
        ));
    }
}
