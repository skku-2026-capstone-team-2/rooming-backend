package com.rooming.domain.property.dto;

import com.rooming.domain.property.entity.TradeType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PropertyDetailResponse {
    private Long propertyId;
    private String title;
    private String address;
    private TradeType tradeType;
    private Integer deposit;
    private Integer monthlyRent;
    private Float areaM2;
    private String roomType;
    private String floorInfo;
    private Integer maintenanceFee;
    private String description;
    private List<String> tags;
    private Boolean has3DModel;
}