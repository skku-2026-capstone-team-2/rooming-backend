package com.rooming.domain.locations.entity.model;

import com.rooming.domain.locations.entity.id.TargetPlacePropertyId;
import com.rooming.domain.locations.entity.type.TRANSPORT_MODE;
import com.rooming.domain.locations.entity.value.Minutes;
import com.rooming.domain.locations.entity.value.Path;
import com.rooming.domain.property.entity.Property;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

@Entity
@Table(name = "routes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Route {

    @EmbeddedId
    private TargetPlacePropertyId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("targetPlaceId")
    @JoinColumn(name = "target_place_id", nullable = false)
    private TargetPlace targetPlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("propertyId")
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", nullable = false)
    private TRANSPORT_MODE transportMode;

    @Column(name = "duration_minutes", nullable = false)
    private Minutes durationMinutes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "route_json", columnDefinition = "jsonb")
    private Path routeJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    public Route(TargetPlace targetPlace, Property property, TRANSPORT_MODE transportMode, Minutes durationMinutes, Path routeJson) {
        if (targetPlace == null || targetPlace.getId() == null) {
            throw new IllegalArgumentException("TargetPlace must be saved before creating routes.");
        }
        if (property == null || property.getPropertyId() == null) {
            throw new IllegalArgumentException("Property must have an id before creating routes.");
        }

        this.targetPlace = targetPlace;
        this.property = property;
        this.id = new TargetPlacePropertyId(targetPlace.getId(), property.getPropertyId());
        this.transportMode = transportMode;
        this.durationMinutes = durationMinutes;
        this.routeJson = routeJson;

        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.createdAt = now;
        this.updatedAt = now;
    }
}