package com.rooming.domain.locations.dto;

import java.util.List;

public record TargetPlaceListData(
        List<TargetPlaceResponseItem> targetPlaces
) {
}