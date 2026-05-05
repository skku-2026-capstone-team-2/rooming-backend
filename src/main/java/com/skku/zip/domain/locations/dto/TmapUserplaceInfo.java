package com.skku.zip.domain.locations.dto;

import com.skku.zip.domain.locations.entity.type.PLACE_CATEGORY;
import com.skku.zip.domain.locations.entity.value.RoadAddress;

public record TmapUserplaceInfo(
        String name,
        PLACE_CATEGORY category,
        RoadAddress address
) {
}
