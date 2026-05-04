package com.skku.zip.domain.locations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.io.Serializable;

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
}
