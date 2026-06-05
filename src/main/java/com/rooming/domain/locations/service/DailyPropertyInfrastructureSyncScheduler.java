package com.rooming.domain.locations.service;

import com.rooming.domain.locations.dto.PropertyInfrastructureSyncResult;
import com.rooming.domain.locations.entity.model.PropertyInfrastructureSyncState;
import com.rooming.domain.locations.repository.InfraAccessibilityRepository;
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
    private final InfraAccessibilityRepository infraAccessibilityRepository;
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
            boolean missingAccessibilities = !infraAccessibilityRepository.existsByPropertyPropertyId(
                    property.getPropertyId()
            );

            try {
                PropertyInfrastructureSyncResult result = missingAccessibilities
                        ? propertyInfrastructureService.syncMissingInfrastructure(property)
                        : propertyInfrastructureService.refreshInfrastructureSelection(property);
                recordSuccess(state, now, result, missingAccessibilities);

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
            boolean missingAccessibilities
    ) {
        if (missingAccessibilities) {
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
}
