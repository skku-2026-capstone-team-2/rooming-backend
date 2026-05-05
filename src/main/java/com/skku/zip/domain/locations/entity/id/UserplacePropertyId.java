package com.skku.zip.domain.locations.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
public class UserplacePropertyId implements Serializable {

    @Column(name = "userplace_id")
    private Long userplaceId;

    @Column(name = "property_id")
    private Long propertyId;

    public UserplacePropertyId() {
    }

    public UserplacePropertyId(Long userplaceId, Long propertyId) {
        this.userplaceId = userplaceId;
        this.propertyId = propertyId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof UserplacePropertyId that)) return false;
        return Objects.equals(userplaceId, that.userplaceId) && Objects.equals(propertyId, that.propertyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userplaceId, propertyId);
    }
}
