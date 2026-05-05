package com.skku.zip.domain.locations.dto;

import com.skku.zip.domain.locations.entity.INFRA_CATEGORY;
import com.skku.zip.domain.locations.entity.RoadAddress;

public record TmapInfrastructureCandidate(
        String externalId,
        String name,
        INFRA_CATEGORY category,
        double latitude,
        double longitude,
        RoadAddress address
) {
}
