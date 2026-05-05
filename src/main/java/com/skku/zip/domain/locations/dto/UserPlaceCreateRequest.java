package com.skku.zip.domain.locations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skku.zip.domain.locations.entity.USER_PLACE_TYPE;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserPlaceCreateRequest(
        @NotNull
        USER_PLACE_TYPE placeType,

        @NotBlank
        String placeName,

        @NotBlank
        String roadAddress,

        @Valid
        @NotNull
        CoordinateDto location,

        String memo,

        @JsonProperty("isActive")
        Boolean isActive
) {
}
