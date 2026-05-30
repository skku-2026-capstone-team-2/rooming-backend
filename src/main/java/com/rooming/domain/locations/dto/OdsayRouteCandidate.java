package com.rooming.domain.locations.dto;

import com.rooming.domain.locations.entity.value.Minutes;
import com.rooming.domain.locations.entity.value.Path;

public record OdsayRouteCandidate(
        Minutes duration,
        Path path
) {
}