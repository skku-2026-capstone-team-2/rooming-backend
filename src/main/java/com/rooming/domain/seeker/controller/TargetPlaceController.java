package com.rooming.domain.seeker.controller;

import com.rooming.common.dto.ApiResponse;
import com.rooming.common.exception.ForbiddenException;
import com.rooming.domain.locations.dto.TargetPlaceCreateRequest;
import com.rooming.domain.locations.dto.TargetPlaceListData;
import com.rooming.domain.locations.dto.TargetPlaceResponseItem;
import com.rooming.domain.locations.dto.TargetPlaceUpdateRequest;
import com.rooming.domain.locations.service.TargetPlaceApiService;
import com.rooming.domain.seeker.entity.Seeker;
import com.rooming.domain.user.entity.AccountType;
import com.rooming.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/seeker/target-place")
public class TargetPlaceController {

    private final TargetPlaceApiService targetPlaceApiService;

    @GetMapping
    public ResponseEntity<ApiResponse<TargetPlaceListData>> getTargetPlaces(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        return ResponseEntity.ok(ApiResponse.success(
                new TargetPlaceListData(targetPlaceApiService.getTargetPlaces(seeker)),
                "Target place list fetched."
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TargetPlaceResponseItem>> createTargetPlace(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody TargetPlaceCreateRequest request
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                targetPlaceApiService.createTargetPlace(seeker, request),
                "Target place saved."
        ));
    }

    @PutMapping("/{targetPlaceId}")
    public ResponseEntity<ApiResponse<TargetPlaceResponseItem>> updateTargetPlace(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long targetPlaceId,
            @Valid @RequestBody TargetPlaceUpdateRequest request
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        return ResponseEntity.ok(ApiResponse.success(
                targetPlaceApiService.updateTargetPlace(seeker, targetPlaceId, request),
                "Target place updated."
        ));
    }

    @DeleteMapping("/{targetPlaceId}")
    public ResponseEntity<Void> deleteTargetPlace(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long targetPlaceId
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        targetPlaceApiService.deleteTargetPlace(seeker, targetPlaceId);
        return ResponseEntity.noContent().build();
    }

    private Seeker requireSeeker(PrincipalDetails principalDetails) {
        if (principalDetails == null || principalDetails.getUser().getAccountType() != AccountType.SEEKER) {
            throw new ForbiddenException("Seeker account is required.");
        }
        return (Seeker) principalDetails.getUser();
    }
}