package com.skku.zip.domain.broker.dto;

import com.skku.zip.domain.locations.dto.CoordinateDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BrokerPropertyCreateRequest(
        @NotBlank
        String title,

        String propertyType,

        String transactionType,

        @NotNull
        Integer depositAmount,

        Integer monthlyRent,

        Integer maintenanceFee,

        @NotNull
        Double areaM2,

        String floorInfo,

        Integer roomCount,

        Integer bathroomCount,

        String direction,

        String availableFrom,

        String description,

        @NotBlank
        String roadAddress,

        @Valid
        @NotNull
        CoordinateDto location
) {
}
