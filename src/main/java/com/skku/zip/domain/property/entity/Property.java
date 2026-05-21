package com.skku.zip.domain.property.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.skku.zip.domain.broker.entity.Broker;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id")
    private Broker broker;

    public void assignBroker(Broker broker) {
        this.broker = broker;
    }
}
