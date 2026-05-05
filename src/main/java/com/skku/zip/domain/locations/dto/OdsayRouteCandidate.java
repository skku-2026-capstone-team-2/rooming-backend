package com.skku.zip.domain.locations.dto;

import com.skku.zip.domain.locations.entity.Minutes;
import com.skku.zip.domain.locations.entity.Path;

public record OdsayRouteCandidate(
        Minutes duration,
        Path path
) {
}
