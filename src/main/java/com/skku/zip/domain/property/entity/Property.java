package com.skku.zip.domain.property.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.skku.zip.domain.broker.entity.Broker;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector")
    private float[] embedding;

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