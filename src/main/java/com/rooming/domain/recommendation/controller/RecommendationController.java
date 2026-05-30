package com.rooming.domain.recommendation.controller;

import com.rooming.common.dto.ApiResponse;
import com.rooming.common.exception.ForbiddenException;
import com.rooming.domain.locations.dto.RouteGeometryDetail;
import com.rooming.domain.recommendation.dto.RecommendationDtos;
import com.rooming.domain.recommendation.service.RecommendationService;
import com.rooming.domain.user.entity.AccountType;
import com.rooming.domain.seeker.entity.Seeker;
import com.rooming.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<ApiResponse<RecommendationDtos.ListData>> getRecommendations(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getRecommendations(seeker),
                "Recommendation list fetched."
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecommendationDtos.Data>> recommend(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody RecommendationDtos.Request request
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.recommend(seeker, request),
                "Recommendations fetched."
        ));
    }

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<RecommendationDtos.FavoriteData>> getFavoriteRecommendations(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getFavoriteRecommendations(seeker),
                "Favorite recommendations fetched."
        ));
    }

    @DeleteMapping("/{recommendationId}")
    public ResponseEntity<Void> deleteRecommendation(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long recommendationId
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        recommendationService.deleteRecommendation(seeker, recommendationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{recommendationId}/favorite")
    public ResponseEntity<ApiResponse<RecommendationDtos.Result>> favoriteRecommendation(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long recommendationId
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.favoriteRecommendation(seeker, recommendationId),
                "Recommendation marked as favorite."
        ));
    }

    @GetMapping("/{recommendationId}/route")
    public ResponseEntity<ApiResponse<RecommendationDtos.RouteDetailData>> getRecommendationRoute(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long recommendationId,
            @RequestParam(defaultValue = "SUMMARY") RouteGeometryDetail detail
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getRecommendationRoute(seeker, recommendationId, detail),
                "Recommendation route fetched."
        ));
    }

    @DeleteMapping("/{recommendationId}/favorite")
    public ResponseEntity<Void> removeFavoriteRecommendation(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long recommendationId
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        recommendationService.removeFavoriteRecommendation(seeker, recommendationId);
        return ResponseEntity.noContent().build();
    }

    private Seeker requireSeeker(PrincipalDetails principalDetails) {
        if (principalDetails == null || principalDetails.getUser().getAccountType() != AccountType.SEEKER) {
            throw new ForbiddenException("Seeker account is required.");
        }
        return (Seeker) principalDetails.getUser();
    }
}