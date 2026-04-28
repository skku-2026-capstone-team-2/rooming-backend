package com.skku.zip.domain.property.controller;

import com.skku.zip.domain.property.dto.AdminCreatePropertyRequest;
import com.skku.zip.domain.property.dto.AdminProperty3DRequest;
import com.skku.zip.domain.property.dto.AdminUpdatePropertyRequest;
import com.skku.zip.domain.property.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/properties")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminPropertyController {

    private final PropertyService propertyService;

    @PostMapping
    public ResponseEntity<?> createProperty(@Valid @RequestBody AdminCreatePropertyRequest request) {
        propertyService.createProperty(request);
        return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "매물 등록 성공"
        ));
    }

    @PatchMapping("/{propertyId}")
    public ResponseEntity<?> updateProperty(
            @PathVariable Long propertyId,
            @Valid @RequestBody AdminUpdatePropertyRequest request
    ) {
        propertyService.updateProperty(propertyId, request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "매물 수정 성공"
        ));
    }

    @PostMapping("/{propertyId}/3d")
    public ResponseEntity<?> update3D(
            @PathVariable Long propertyId,
            @Valid @RequestBody AdminProperty3DRequest request
    ) {
        propertyService.update3DAsset(propertyId, request);
        return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "3D 자산 등록 성공"
        ));
    }
}
