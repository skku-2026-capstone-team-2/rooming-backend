package com.skku.zip.domain.recommendation.controller;

import com.skku.zip.common.dto.ApiResponse;
import com.skku.zip.domain.recommendation.dto.RecommendationDtos;
import com.skku.zip.domain.recommendation.service.RecommendationService;
import com.skku.zip.domain.user.entity.AccountType;
import com.skku.zip.domain.seeker.entity.Seeker;
import com.skku.zip.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ApiResponse<RecommendationDtos.ListData> getRecommendations(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        return ApiResponse.success(
                recommendationService.getRecommendations(seeker),
                "Recommendation list fetched."
        );
    }

    @PostMapping
    public ApiResponse<RecommendationDtos.Data> recommend(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody RecommendationDtos.Request request
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        return ApiResponse.success(
                recommendationService.recommend(seeker, request),
                "Recommendations fetched."
        );
    }

    @GetMapping("/favorites")
    public ApiResponse<RecommendationDtos.FavoriteData> getFavoriteRecommendations(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        return ApiResponse.success(
                recommendationService.getFavoriteRecommendations(seeker),
                "Favorite recommendations fetched."
        );
    }

    @DeleteMapping("/{recommendationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecommendation(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long recommendationId
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        recommendationService.deleteRecommendation(seeker, recommendationId);
    }

    @PostMapping("/{recommendationId}/favorite")
    public ApiResponse<RecommendationDtos.Result> favoriteRecommendation(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long recommendationId
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        return ApiResponse.success(
                recommendationService.favoriteRecommendation(seeker, recommendationId),
                "Recommendation marked as favorite."
        );
    }

    @DeleteMapping("/{recommendationId}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavoriteRecommendation(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long recommendationId
    ) {
        Seeker seeker = requireSeeker(principalDetails);
        recommendationService.removeFavoriteRecommendation(seeker, recommendationId);
    }

    private Seeker requireSeeker(PrincipalDetails principalDetails) {
        if (principalDetails == null || principalDetails.getUser().getAccountType() != AccountType.SEEKER) {
            throw new AccessDeniedException("Seeker account is required.");
        }
        return (Seeker) principalDetails.getUser();
    }
}
