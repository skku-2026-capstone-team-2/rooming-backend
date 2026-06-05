package com.rooming.domain.locations.entity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tmap_poi_sync_state")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TmapPoiSyncState {

    public static final String GLOBAL_ID = "global";

    @Id
    @Column(name = "state_id", nullable = false, length = 64)
    private String id;

    @Column(name = "quota_exhausted_on")
    private LocalDate quotaExhaustedOn;

    @Column(name = "quota_exhausted_at")
    private Instant quotaExhaustedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public TmapPoiSyncState(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank.");
        }
        this.id = id;
    }

    public boolean isQuotaExhaustedOn(LocalDate date) {
        return quotaExhaustedOn != null && quotaExhaustedOn.equals(date);
    }

    public void markQuotaExhausted(LocalDate date, Instant now) {
        this.quotaExhaustedOn = date;
        this.quotaExhaustedAt = now;
    }
}
