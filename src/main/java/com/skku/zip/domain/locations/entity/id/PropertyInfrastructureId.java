package com.skku.zip.domain.locations.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
public class PropertyInfrastructureId implements Serializable {

    @Column(name = "property_id")
    private Long propertyId;

    @Column(name = "infrastructure_id")
    private Long infrastructureId;

    public PropertyInfrastructureId(Long propertyId, Long infrastructureId) {
        this.propertyId = propertyId;
        this.infrastructureId = infrastructureId;
    }

    public PropertyInfrastructureId() {

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof PropertyInfrastructureId that)) return false;
        return Objects.equals(this.propertyId, that.propertyId) && Objects.equals(this.infrastructureId, that.infrastructureId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyId, infrastructureId);
    }
}
