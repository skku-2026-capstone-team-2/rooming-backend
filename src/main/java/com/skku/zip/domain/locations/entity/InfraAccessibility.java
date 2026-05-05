package com.skku.zip.domain.locations.entity;

import com.skku.zip.domain.property.entity.Property;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "infra_accessibility")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InfraAccessibility {

    @EmbeddedId
    private PropertyInfraId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("propertyId")
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("infrastructureId")
    @JoinColumn(name = "infrastructure_id", nullable = false)
    private Infrastructure infrastructure;

    private Minutes walkingTime;

    public InfraAccessibility(Property property, Infrastructure infrastructure, Minutes walkingTime) {
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
        this.id = new PropertyInfraId(property.getPropertyId(), infrastructure.getId());
        this.walkingTime = walkingTime;
    }
}
