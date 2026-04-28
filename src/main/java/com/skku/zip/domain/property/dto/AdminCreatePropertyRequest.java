package com.skku.zip.domain.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminCreatePropertyRequest {
    @NotBlank private String title;
    @NotBlank private String address;
    @NotNull  private Double latitude;
    @NotNull  private Double longitude;
    @NotNull  private Integer deposit;
    @NotNull  private Integer monthlyRent;
    private Integer maintenanceFee;
    private Float areaM2;
    private String roomType;
    private String floorInfo;
    private String description;
}
