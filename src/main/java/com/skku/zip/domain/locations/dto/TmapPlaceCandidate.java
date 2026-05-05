package com.skku.zip.domain.locations.dto;

public record TmapPlaceCandidate(
        String externalId,
        String placeName,
        String roadAddress,
        double latitude,
        double longitude
) {
}
