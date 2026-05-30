package com.skku.zip.domain.locations.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.skku.zip.domain.locations.entity.type.PLACE_CATEGORY;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TargetPlaceCreateRequest(
        @JsonAlias("placeType")
        @NotNull
        PLACE_CATEGORY category,

        @NotBlank
        String placeName,

        @NotBlank
        String roadAddress,

        @Valid
        @NotNull
        CoordinateDto location,

        String memo
) {
}
