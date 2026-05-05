package com.skku.zip.domain.locations.dto;

public record PlaceSearchItem(
        String placeName,
        String roadAddress,
        CoordinateDto location
) {
}
