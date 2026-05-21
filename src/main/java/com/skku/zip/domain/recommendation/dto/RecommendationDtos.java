package com.skku.zip.domain.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.skku.zip.domain.locations.dto.CoordinateDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Objects;

public final class RecommendationDtos {

    private RecommendationDtos() {
    }

    public record Request(
            @NotBlank
            String query,

            List<String> preferences,

            @Min(1)
            @Max(5)
            Integer topN
    ) {
        public List<String> normalizedPreferences() {
            if (preferences == null) {
                return List.of();
            }
            return preferences.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
        }

        public int normalizedTopN() {
            return topN == null ? 3 : topN;
        }

        public String normalizedQuery() {
            return query.trim();
        }
    }

    public record Data(
            String message,
            List<Result> results
    ) {
    }

    public record ListData(
            List<Result> results
    ) {
    }

    public record FavoriteData(
            List<Result> results
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            Long recommendationId,
            Long propertyId,
            Boolean favorite,
            PropertyDetails property,
            TargetPlaceRoute firstTargetPlaceRoute,
            List<InfrastructureDetails> infrastructures,
            String explanation
    ) {
    }

    public record PropertyDetails(
            CoordinateDto location,
            Integer depositAmount,
            Integer monthlyRent,
            Integer maintenanceFee
    ) {
    }

    public record TargetPlaceRoute(
            Long targetPlaceId,
            String transportMode,
            Integer durationMinutes,
            Object routeJson
    ) {
    }

    public record InfrastructureDetails(
            Long infrastructureId,
            String name,
            String category,
            String roadAddress,
            CoordinateDto location,
            Integer walkingMinutes
    ) {
    }
}
