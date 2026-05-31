package com.rooming.domain.broker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.rooming.domain.locations.dto.CoordinateDto;
import com.rooming.domain.property.entity.TradeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BrokerPropertyCreateRequest(
        @NotBlank
        String title,

        String propertyType,

        @JsonAlias("trade_type")
        @NotNull
        TradeType tradeType,

        @NotNull
        Integer depositAmount,

        Integer monthlyRent,

        List<String> tags,

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
        CoordinateDto location,

        String splineUrl
) {
}