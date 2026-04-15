package com.skku.zip.domain.property.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// domain/property/entity/Property.java
@Data
@Builder
public class Property {
    private Long propertyId;
    private String title;
    private String address;
    private Double latitude;
    private Double longitude;
    private Integer deposit;
    private Integer monthlyRent;
    private Float areaM2;
    private String roomType;
    private String floorInfo;
    private Boolean has3DModel;
    private String splineUrl;
    private List<String> imageUrls; // 매물 사진 여러개?
}