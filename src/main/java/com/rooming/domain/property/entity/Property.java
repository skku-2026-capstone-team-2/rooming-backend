package com.rooming.domain.property.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rooming.domain.broker.entity.Broker;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type")
    private TradeType tradeType;
    private Integer deposit;
    private Integer monthlyRent;
    private Float areaM2;
    private String roomType;
    private String floorInfo;
    private Integer maintenanceFee;
    @Column(columnDefinition = "text")
    private String description;
    private List<String> tags;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Column(columnDefinition = "vector", insertable = false, updatable = false)
    private String embedding;

    private Boolean has3DModel;
    private String splineUrl;
    private List<String> imageUrls;
    @Builder.Default
    @Column(name = "nearby_infrastructures_fetched")
    private Boolean nearbyInfrastructuresFetched = false;
    @Builder.Default
    @Column(name = "infra_accessibilities_fetched")
    private Boolean infraAccessibilitiesFetched = false;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id")
    private Broker broker;

    public void assignBroker(Broker broker) {
        this.broker = broker;
    }

    public boolean nearbyInfrastructuresFetched() {
        return Boolean.TRUE.equals(nearbyInfrastructuresFetched);
    }

    public boolean infraAccessibilitiesFetched() {
        return Boolean.TRUE.equals(infraAccessibilitiesFetched);
    }

    public void markNearbyInfrastructuresFetched(boolean fetched) {
        this.nearbyInfrastructuresFetched = fetched;
    }

    public void markInfraAccessibilitiesFetched(boolean fetched) {
        this.infraAccessibilitiesFetched = fetched;
    }

    @PrePersist
    void initializeInfrastructureSyncFlags() {
        if (nearbyInfrastructuresFetched == null) {
            nearbyInfrastructuresFetched = false;
        }
        if (infraAccessibilitiesFetched == null) {
            infraAccessibilitiesFetched = false;
        }
    }
}
