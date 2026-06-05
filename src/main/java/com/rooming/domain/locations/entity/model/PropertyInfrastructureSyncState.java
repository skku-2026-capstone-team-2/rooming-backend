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

@Entity
@Table(name = "property_infrastructure_sync_state")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyInfrastructureSyncState {

    @Id
    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "missing_sync_attempt_count", nullable = false)
    private int missingSyncAttemptCount;

    @Column(name = "refresh_attempt_count", nullable = false)
    private int refreshAttemptCount;

    @Column(name = "last_missing_synced_at")
    private Instant lastMissingSyncedAt;

    @Column(name = "last_refreshed_at")
    private Instant lastRefreshedAt;

    @Column(name = "last_quota_stopped_at")
    private Instant lastQuotaStoppedAt;

    @Column(name = "last_failed_at")
    private Instant lastFailedAt;

    @Column(name = "last_infrastructure_count", nullable = false)
    private int lastInfrastructureCount;

    @Column(name = "last_created_accessibility_count", nullable = false)
    private int lastCreatedAccessibilityCount;

    @Column(name = "last_removed_accessibility_count", nullable = false)
    private int lastRemovedAccessibilityCount;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public PropertyInfrastructureSyncState(Long propertyId) {
        if (propertyId == null) {
            throw new IllegalArgumentException("propertyId must not be null.");
        }
        this.propertyId = propertyId;
    }

    public void recordMissingSync(Instant now, int infrastructureCount, int createdAccessibilityCount) {
        this.missingSyncAttemptCount++;
        this.lastMissingSyncedAt = now;
        this.lastFailedAt = null;
        recordCounts(infrastructureCount, createdAccessibilityCount, 0);
    }

    public void recordRefresh(
            Instant now,
            int infrastructureCount,
            int createdAccessibilityCount,
            int removedAccessibilityCount
    ) {
        this.refreshAttemptCount++;
        this.lastRefreshedAt = now;
        this.lastFailedAt = null;
        recordCounts(infrastructureCount, createdAccessibilityCount, removedAccessibilityCount);
    }

    public void recordQuotaStopped(Instant now, int infrastructureCount, int createdAccessibilityCount) {
        this.lastQuotaStoppedAt = now;
        recordCounts(infrastructureCount, createdAccessibilityCount, 0);
    }

    public void recordFailure(Instant now) {
        this.lastFailedAt = now;
    }

    private void recordCounts(
            int infrastructureCount,
            int createdAccessibilityCount,
            int removedAccessibilityCount
    ) {
        this.lastInfrastructureCount = Math.max(0, infrastructureCount);
        this.lastCreatedAccessibilityCount = Math.max(0, createdAccessibilityCount);
        this.lastRemovedAccessibilityCount = Math.max(0, removedAccessibilityCount);
    }
}
