package com.skku.zip.domain.seeker.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
public class SeekerTargetPlaceId implements Serializable {

    @Column(name = "seeker_id")
    private Long seekerId;

    @Column(name = "target_place_id")
    private Long targetPlaceId;

    public SeekerTargetPlaceId() {
    }

    public SeekerTargetPlaceId(Long seekerId, Long targetPlaceId) {
        this.seekerId = seekerId;
        this.targetPlaceId = targetPlaceId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof SeekerTargetPlaceId that)) return false;
        return Objects.equals(seekerId, that.seekerId) && Objects.equals(targetPlaceId, that.targetPlaceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seekerId, targetPlaceId);
    }
}
