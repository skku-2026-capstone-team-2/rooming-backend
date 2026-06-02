package com.rooming.domain.property.controller;

import com.rooming.common.dto.ApiResponse;
import com.rooming.common.exception.ForbiddenException;
import com.rooming.domain.broker.entity.Broker;
import com.rooming.domain.property.dto.Property3DResponse;
import com.rooming.domain.property.dto.PropertyDetailResponse;
import com.rooming.domain.property.dto.PropertyImageResponse;
import com.rooming.domain.property.entity.Property;
import com.rooming.domain.property.service.PropertyService;
import com.rooming.domain.user.entity.AccountType;
import com.rooming.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateImages(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long id,
            @RequestPart("images") List<MultipartFile> images
    ) {
        Broker broker = requireBroker(principalDetails);
        List<PropertyImageResponse> propertyImages = propertyService.updateImages(id, broker, images);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "propertyId", id,
                        "images", propertyImages
                ),
                "Property images saved."
        ));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteImage(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long id,
            @PathVariable Integer imageId
    ) {
        Broker broker = requireBroker(principalDetails);
        List<PropertyImageResponse> images = propertyService.deleteImage(id, broker, imageId);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "propertyId", id,
                        "images", images
                ),
                "Property image deleted."
        ));
    }

    @GetMapping("/{id}/3d")
    public ResponseEntity<ApiResponse<Property3DResponse>> get3D(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                propertyService.get3D(id),
                "3D model fetched."
        ));
    }

    private Broker requireBroker(PrincipalDetails principalDetails) {
        if (principalDetails == null || principalDetails.getUser().getAccountType() != AccountType.BROKER) {
            throw new ForbiddenException("Broker account is required.");
        }
        return (Broker) principalDetails.getUser();
    }
}
