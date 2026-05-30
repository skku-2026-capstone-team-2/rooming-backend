package com.rooming.domain.locations.dto;

public record TargetPlaceResponseItem(
        Long targetPlaceId,
        String category,
        String placeName,
        String roadAddress,
        CoordinateDto location,
        String memo
) {
}