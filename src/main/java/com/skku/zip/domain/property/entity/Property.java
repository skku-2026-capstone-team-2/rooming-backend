package com.skku.zip.domain.property.entity;

import com.skku.zip.domain.broker.entity.Broker;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@Entity
@Table(name = "properties")
@NoArgsConstructor
@AllArgsConstructor
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id")
    private Broker broker;

    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    @ElementCollection
    private List<String> tags;

    @ElementCollection
    private List<String> imageUrls;
}
