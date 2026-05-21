package com.skku.zip.domain.locations.dto;

public record TargetPlaceResponseItem(
        Long targetPlaceId,
        String category,
        String placeName,
        String roadAddress,
        CoordinateDto location,
        String memo
) {
}
