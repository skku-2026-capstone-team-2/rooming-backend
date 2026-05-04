package com.skku.zip.domain.locations.entity;

import com.skku.zip.domain.property.entity.Property;
import jakarta.persistence.*;

@Entity
@Table(name = "infra_property_time")
public class PropertyInfraTime {

    @EmbeddedId
    private PropertyInfrastructureId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("property_id")
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("infrastructure_id")
    @JoinColumn(name = "infrastructure_id", nullable = false)
    private Infrastructure infrastructure;

    private Minutes walkingTime;
}
