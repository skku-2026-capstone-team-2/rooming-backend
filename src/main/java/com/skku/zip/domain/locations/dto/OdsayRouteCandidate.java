package com.skku.zip.domain.locations.dto;

import com.skku.zip.domain.locations.entity.value.Minutes;
import com.skku.zip.domain.locations.entity.value.Path;

public record OdsayRouteCandidate(
        Minutes duration,
        Path path
) {
}
