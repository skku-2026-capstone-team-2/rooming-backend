package com.skku.zip.domain.property.controller;

import com.skku.zip.domain.property.entity.Property;
import com.skku.zip.domain.property.service.PropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/properties")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    public ResponseEntity<?> getAll() {

        return ResponseEntity.ok(propertyService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", propertyService.getById(id),
                "message", "매물 상세 조회 성공"
        ));
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<?> getImages(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "propertyId", id,
                        "images", propertyService.getImages(id)
                ),
                "message", "매물 이미지 조회 성공"
        ));
    }

    @GetMapping("/{id}/3d")
    public ResponseEntity<?> get3D(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", propertyService.get3D(id),
                "message", "3D 모델 조회 성공"
        ));
    }

}