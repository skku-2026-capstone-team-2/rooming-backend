package com.rooming.domain.locations.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.rooming.domain.locations.entity.type.PLACE_CATEGORY;
import jakarta.validation.Valid;

public record TargetPlaceUpdateRequest(
        @JsonAlias("placeType")
        PLACE_CATEGORY category,
        String placeName,
        String roadAddress,

        @Valid
        CoordinateDto location,

        String memo
) {
}