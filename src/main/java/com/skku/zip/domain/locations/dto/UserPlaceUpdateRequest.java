package com.skku.zip.domain.locations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skku.zip.domain.locations.entity.USER_PLACE_TYPE;
import jakarta.validation.Valid;

public record UserPlaceUpdateRequest(
        USER_PLACE_TYPE placeType,
        String placeName,
        String roadAddress,

        @Valid
        CoordinateDto location,

        String memo,

        @JsonProperty("isActive")
        Boolean isActive
) {
}
