package com.skku.zip.domain.locations.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
public class TargetPlacePropertyId implements Serializable {

    @Column(name = "target_place_id")
    private Long targetPlaceId;

    @Column(name = "property_id")
    private Long propertyId;

    public TargetPlacePropertyId() {
    }

    public TargetPlacePropertyId(Long targetPlaceId, Long propertyId) {
        this.targetPlaceId = targetPlaceId;
        this.propertyId = propertyId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof TargetPlacePropertyId that)) return false;
        return Objects.equals(targetPlaceId, that.targetPlaceId) && Objects.equals(propertyId, that.propertyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetPlaceId, propertyId);
    }
}
