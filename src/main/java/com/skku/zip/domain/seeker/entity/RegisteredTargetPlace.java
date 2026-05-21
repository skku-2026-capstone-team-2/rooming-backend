package com.skku.zip.domain.seeker.entity;

import com.skku.zip.domain.locations.entity.model.TargetPlace;
import com.skku.zip.domain.seeker.entity.id.SeekerTargetPlaceId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "registered_target_place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegisteredTargetPlace {

    @EmbeddedId
    private SeekerTargetPlaceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("seekerId")
    @JoinColumn(name = "seeker_id", nullable = false)
    private Seeker seeker;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("targetPlaceId")
    @JoinColumn(name = "target_place_id", nullable = false)
    private TargetPlace targetPlace;

    @Column
    private String memo;

    public RegisteredTargetPlace(Seeker seeker, TargetPlace targetPlace, String memo) {
        if (seeker == null || seeker.getId() == null) {
            throw new IllegalArgumentException("Seeker must be saved before adding target places.");
        }
        if (targetPlace == null || targetPlace.getId() == null) {
            throw new IllegalArgumentException("Target place must be saved before adding it to a seeker.");
        }

        this.id = new SeekerTargetPlaceId(seeker.getId(), targetPlace.getId());
        this.seeker = seeker;
        this.targetPlace = targetPlace;
        updateMemo(memo);
    }

    public void updateMemo(String memo) {
        this.memo = normalizeMemo(memo);
    }

    public boolean hasTargetPlaceId(Long targetPlaceId) {
        return id != null && Objects.equals(id.getTargetPlaceId(), targetPlaceId);
    }

    private String normalizeMemo(String memo) {
        return memo == null || memo.isBlank() ? null : memo.trim();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof RegisteredTargetPlace that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
