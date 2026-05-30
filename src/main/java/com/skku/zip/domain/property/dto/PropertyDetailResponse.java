package com.skku.zip.domain.property.dto;

import com.skku.zip.domain.property.entity.TradeType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PropertyDetailResponse {
    private Long propertyId;
    private String title;
    private String address;
    private Integer deposit;
    private Integer monthlyRent;
    private Float areaM2;
    private String roomType;
    private String floorInfo;
    private Boolean has3DModel;
    private TradeType tradeType;
    private List<String> tags;
    private String description;
}
