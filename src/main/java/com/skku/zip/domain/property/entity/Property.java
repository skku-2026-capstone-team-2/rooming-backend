package com.skku.zip.domain.property.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// domain/property/entity/Property.java
@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Property {
    @Id
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
    private Integer maintenanceFee;
    private String description;
    private Boolean has3DModel;
    private String splineUrl;
    private List<String> imageUrls;
}
