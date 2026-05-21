package com.skku.zip.domain.broker.dto;

import com.skku.zip.domain.locations.dto.CoordinateDto;

public record BrokerPropertyData(
        Long propertyId,
        String title,
        String propertyType,
        String roadAddress,
        CoordinateDto location,
        Integer depositAmount,
        Integer monthlyRent,
        Integer maintenanceFee,
        Double areaM2,
        String floorInfo,
        String description,
        Boolean hasProperty3D
) {
}
