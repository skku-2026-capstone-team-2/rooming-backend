package com.skku.zip.domain.locations.dto;

import java.util.List;

public record TargetPlaceListData(
        List<TargetPlaceResponseItem> targetPlaces
) {
}
