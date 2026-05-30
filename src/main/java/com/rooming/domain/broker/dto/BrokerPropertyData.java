package com.rooming.domain.broker.dto;

import com.rooming.domain.locations.dto.CoordinateDto;
import com.rooming.domain.property.entity.TradeType;

import java.util.List;

public record BrokerPropertyData(
        Long propertyId,
        String title,
        String propertyType,
        String roadAddress,
        CoordinateDto location,
        TradeType tradeType,
        Integer depositAmount,
        Integer monthlyRent,
        Integer maintenanceFee,
        Double areaM2,
        String floorInfo,
        String description,
        List<String> tags,
        Boolean hasProperty3D
) {
    public BrokerPropertyData {
        if (tradeType == TradeType.DEPOSIT_BASIS) {
            monthlyRent = 0;
        }
    }
}