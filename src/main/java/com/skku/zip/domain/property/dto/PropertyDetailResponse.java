package com.skku.zip.domain.property.dto;

import lombok.Builder;
import lombok.Data;

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
}
