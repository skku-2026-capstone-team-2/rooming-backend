package com.skku.zip.domain.locations.entity.model;

import com.skku.zip.domain.locations.entity.id.UserplacePropertyId;
import com.skku.zip.domain.locations.entity.type.TRANSPORT_MODE;
import com.skku.zip.domain.locations.entity.value.Minutes;
import com.skku.zip.domain.locations.entity.value.Path;
import com.skku.zip.domain.property.entity.Property;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

@Entity
@Table(name = "routes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Route {

    @EmbeddedId
    private UserplacePropertyId userplacePropertyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userplaceId")
    @JoinColumn(name = "userplace_id", nullable = false)
    private Userplace userPlace;

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

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    public Route(Userplace userPlace, Property property, TRANSPORT_MODE transportMode, Minutes durationMinutes, Path routeJson) {
        if (userPlace == null || userPlace.getId() == null) {
            throw new IllegalArgumentException("Userplace must be saved before creating routes.");
        }
        if (property == null || property.getPropertyId() == null) {
            throw new IllegalArgumentException("Property must have an id before creating routes.");
        }

        this.userPlace = userPlace;
        this.property = property;
        this.userplacePropertyId = new UserplacePropertyId(userPlace.getId(), property.getPropertyId());
        this.transportMode = transportMode;
        this.durationMinutes = durationMinutes;
        this.routeJson = routeJson;

        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.createdAt = now;
        this.updatedAt = now;
    }
}
