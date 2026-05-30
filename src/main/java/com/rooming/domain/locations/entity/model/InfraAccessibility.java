package com.rooming.domain.locations.entity.model;

import com.rooming.domain.locations.entity.id.PropertyInfrastructureId;
import com.rooming.domain.locations.entity.value.Minutes;
import com.rooming.domain.locations.entity.value.Path;
import com.rooming.domain.property.entity.Property;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "infra_accessibilities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InfraAccessibility {

    @EmbeddedId
    private PropertyInfrastructureId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("propertyId")
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("infrastructureId")
    @JoinColumn(name = "infrastructure_id", nullable = false)
    private Infrastructure infrastructure;

    @Column
    private Minutes walkingTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "walking_route_json", columnDefinition = "jsonb")
    private Path walkingRouteJson;

    public InfraAccessibility(Property property, Infrastructure infrastructure, Minutes walkingTime, Path walkingRouteJson) {
        if (property == null || property.getPropertyId() == null) {
            throw new IllegalArgumentException("Property must have an id before creating infrastructure accessibility.");
        }
        if (infrastructure == null || infrastructure.getId() == null) {
            throw new IllegalArgumentException("Infrastructure must be saved before creating infrastructure accessibility.");
        }
        if (walkingTime == null) {
               throw new IllegalArgumentException("Walking time must not be null.");
        }

        this.property = property;
        this.infrastructure = infrastructure;
        this.id = new PropertyInfrastructureId(property.getPropertyId(), infrastructure.getId());
        this.walkingTime = walkingTime;
        this.walkingRouteJson = walkingRouteJson;
    }
}