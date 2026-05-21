package com.skku.zip.domain.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class AiRecommendationDtos {

    private AiRecommendationDtos() {
    }

    public record Request(
            String query,
            List<String> preferences,
            @JsonProperty("target_places")
            List<Long> targetPlaces,
            @JsonProperty("top_n")
            int topN
    ) {
    }

    public record Response(
            Boolean success,
            String message,
            List<Result> results
    ) {
    }

    public record Result(
            @JsonProperty("property_id")
            Long propertyId,
            @JsonProperty("infra_ids")
            List<Long> infraIds,
            String explanation
    ) {
    }
}
