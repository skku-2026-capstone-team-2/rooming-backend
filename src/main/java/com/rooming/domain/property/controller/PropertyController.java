package com.rooming.domain.property.controller;

import com.rooming.common.dto.ApiResponse;
import com.rooming.domain.property.dto.Property3DResponse;
import com.rooming.domain.property.dto.PropertyDetailResponse;
import com.rooming.domain.property.dto.PropertyImageResponse;
import com.rooming.domain.property.entity.Property;
import com.rooming.domain.property.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Property>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                propertyService.getAll(),
                "Property list fetched."
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                propertyService.getById(id),
                "Property detail fetched."
        ));
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getImages(@PathVariable Long id) {
        List<PropertyImageResponse> images = propertyService.getImages(id);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "propertyId", id,
                        "images", images
                ),
                "Property images fetched."
        ));
    }

    @GetMapping("/{id}/3d")
    public ResponseEntity<ApiResponse<Property3DResponse>> get3D(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                propertyService.get3D(id),
                "3D model fetched."
        ));
    }
}