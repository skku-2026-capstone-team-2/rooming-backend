package com.skku.zip.domain.locations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserPlaceResponseItem(
        Long userPlaceId,
        String placeType,
        String placeName,
        String roadAddress,
        CoordinateDto location,
        String memo,
        @JsonProperty("isActive")
        boolean isActive
) {
}
