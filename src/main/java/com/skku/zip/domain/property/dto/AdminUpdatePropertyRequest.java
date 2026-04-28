package com.skku.zip.domain.property.dto;

import lombok.Data;

@Data
public class AdminUpdatePropertyRequest {
    private String title;
    private String address;
    private Integer deposit;
    private Integer monthlyRent;
    private Integer maintenanceFee;
    private Float areaM2;
    private String roomType;
    private String floorInfo;
    private String description;
}
