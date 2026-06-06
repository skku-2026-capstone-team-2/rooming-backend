package com.rooming.domain.locations.service;

import com.rooming.domain.locations.dto.PropertyInfrastructureSyncResult;
import com.rooming.domain.locations.entity.model.PropertyInfrastructureSyncState;
import com.rooming.domain.locations.repository.PropertyInfrastructureSyncStateRepository;
import com.rooming.domain.property.entity.Property;
import com.rooming.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyPropertyInfrastructureSyncScheduler {

    private final PropertyRepository propertyRepository;
    private final PropertyInfrastructureService propertyInfrastructureService;
    private final PropertyInfrastructureSyncStateRepository syncStateRepository;
    private final TmapPoiSyncQuotaService quotaService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${rooming.infrastructure-sync.daily.enabled:true}")
    private boolean enabled;

    @Scheduled(
            cron = "${rooming.infrastructure-sync.daily.cron:0 0 3 * * *}",
            zone = "Asia/Seoul"
    )
    @EventListener(ApplicationReadyEvent.class)
    public void syncAllPropertyInfrastructures() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            syncAllPropertyInfrastructuresIfAllowed();
        } finally {
            running.set(false);
        }
    }

    private void syncAllPropertyInfrastructuresIfAllowed() {
        if (!enabled || quotaService.isQuotaExhaustedToday()) {
            return;
        }

        for (Property property : propertyRepository.findPropertiesForDailyInfrastructureSync()) {
            PropertyInfrastructureSyncState state = syncState(property);
            Instant now = Instant.now();
            SyncMode syncMode = syncMode(property);

            try {
                PropertyInfrastructureSyncResult result = switch (syncMode) {
                    case NEARBY_INFRASTRUCTURE ->
                            propertyInfrastructureService.syncNearbyInfrastructures(property);
                    case INFRA_ACCESSIBILITY ->
                            propertyInfrastructureService.syncMissingAccessibilities(property);
                    case REFRESH ->
                            propertyInfrastructureService.refreshInfrastructureSelection(property);
                };
                recordSuccess(state, now, result, syncMode);

                if (result.quotaExceeded()) {
                    state.recordQuotaStopped(
                            now,
                            result.infrastructureCount(),
                            result.createdAccessibilityCount()
                    );
                    syncStateRepository.save(state);
                    quotaService.markQuotaExhaustedNow();
                    log.warn(
                            "Stopped daily infrastructure sync because TMAP POI quota appears exhausted. propertyId={}",
                            property.getPropertyId()
                    );
                    break;
                }

                syncStateRepository.save(state);
            } catch (RuntimeException e) {
                state.recordFailure(now);
                syncStateRepository.save(state);
                log.warn(
                        "Daily infrastructure sync failed for propertyId={}: {}",
                        property.getPropertyId(),
                        e.getMessage()
                );
            }
        }
    }

    private void recordSuccess(
            PropertyInfrastructureSyncState state,
            Instant now,
            PropertyInfrastructureSyncResult result,
            SyncMode syncMode
    ) {
        if (syncMode != SyncMode.REFRESH) {
            state.recordMissingSync(
                    now,
                    result.infrastructureCount(),
                    result.createdAccessibilityCount()
            );
            return;
        }

        state.recordRefresh(
                now,
                result.infrastructureCount(),
                result.createdAccessibilityCount(),
                result.removedAccessibilityCount()
        );
    }

    private PropertyInfrastructureSyncState syncState(Property property) {
        return syncStateRepository.findById(property.getPropertyId())
                .orElseGet(() -> new PropertyInfrastructureSyncState(property.getPropertyId()));
    }

    private SyncMode syncMode(Property property) {
        if (!property.nearbyInfrastructuresFetched()) {
            return SyncMode.NEARBY_INFRASTRUCTURE;
        }
        if (!property.infraAccessibilitiesFetched()) {
            return SyncMode.INFRA_ACCESSIBILITY;
        }
        return SyncMode.REFRESH;
    }

    private enum SyncMode {
        NEARBY_INFRASTRUCTURE,
        INFRA_ACCESSIBILITY,
        REFRESH
    }
}
