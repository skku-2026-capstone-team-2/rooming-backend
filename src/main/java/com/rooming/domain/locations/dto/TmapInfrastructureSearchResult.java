package com.rooming.domain.locations.dto;

import com.rooming.domain.locations.entity.type.INFRA_CATEGORY;

import java.util.List;
import java.util.Set;

public record TmapInfrastructureSearchResult(
        List<TmapInfrastructureCandidate> candidates,
        Set<INFRA_CATEGORY> completedCategories,
        boolean quotaExceeded
) {
    public TmapInfrastructureSearchResult {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        completedCategories = completedCategories == null ? Set.of() : Set.copyOf(completedCategories);
    }
}
