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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyPropertyInfrastructureSyncScheduler {

    private final PropertyRepository propertyRepository;
    private final PropertyInfrastructureService propertyInfrastructureService;
    private final PropertyInfrastructureSyncStateRepository syncStateRepository;
    private final TmapPoiSyncQuotaService poiQuotaService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${rooming.infrastructure-sync.daily.enabled:true}")
    private boolean enabled;

    @Scheduled(
            cron = "${rooming.infrastructure-sync.daily.cron:0 0 3 * * *}",
            zone = "Asia/Seoul"
    )
    public void syncAllPropertyInfrastructures() {
        runSync("scheduled");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncAllPropertyInfrastructuresOnStartup() {
        runSync("application-ready");
    }

    private void runSync(String trigger) {
        if (!running.compareAndSet(false, true)) {
            log.info(
                    "Skipping daily property infrastructure sync because another run is already active. trigger={}",
                    trigger
            );
            return;
        }

        long startedNanos = System.nanoTime();
        log.info("Starting daily property infrastructure sync. trigger={}", trigger);
        try {
            SyncRunStats stats = syncAllPropertyInfrastructuresIfAllowed(trigger);
            log.info(
                    "Finished daily property infrastructure sync. trigger={}, status={}, propertiesFound={}, "
                            + "poiProcessed={}, accessibilityProcessed={}, succeeded={}, failed={}, "
                            + "poiQuotaExceeded={}, walkingRouteQuotaExceeded={}, totalInfrastructures={}, "
                            + "createdAccessibilities={}, removedAccessibilities={}, elapsedMs={}",
                    trigger,
                    stats.status,
                    stats.propertiesFound,
                    stats.poiProcessedCount,
                    stats.accessibilityProcessedCount,
                    stats.successCount,
                    stats.failureCount,
                    stats.poiQuotaExceeded,
                    stats.walkingRouteQuotaExceeded,
                    stats.totalInfrastructureCount,
                    stats.totalCreatedAccessibilityCount,
                    stats.totalRemovedAccessibilityCount,
                    elapsedMillis(startedNanos)
            );
        } catch (RuntimeException e) {
            log.error(
                    "Daily property infrastructure sync aborted. trigger={}, elapsedMs={}",
                    trigger,
                    elapsedMillis(startedNanos),
                    e
            );
            throw e;
        } finally {
            running.set(false);
        }
    }

    private SyncRunStats syncAllPropertyInfrastructuresIfAllowed(String trigger) {
        if (!enabled) {
            log.info("Skipping daily property infrastructure sync because it is disabled. trigger={}", trigger);
            return SyncRunStats.skipped(SyncRunStatus.SKIPPED_DISABLED);
        }

        List<Property> properties = propertyRepository.findPropertiesForDailyInfrastructureSync();
        SyncRunStats stats = new SyncRunStats(properties.size());
        log.info(
                "Loaded properties for daily infrastructure sync. trigger={}, propertyCount={}",
                trigger,
                properties.size()
        );

        boolean poiQuotaAlreadyExceeded = poiQuotaService.isQuotaExhaustedToday();
        if (poiQuotaAlreadyExceeded) {
            stats.markPoiQuotaExceeded();
            log.info(
                    "Skipping TMAP POI phase because POI quota is already exhausted today. "
                            + "Accessibility phase will still run from stored infrastructures. trigger={}",
                    trigger
            );
        } else {
            runPoiPhase(trigger, properties, stats);
        }

        runAccessibilityPhase(trigger, properties, stats);
        return stats;
    }

    private void runPoiPhase(String trigger, List<Property> properties, SyncRunStats stats) {
        log.info("Starting TMAP POI infrastructure sync phase. trigger={}, propertyCount={}", trigger, properties.size());
        for (Property property : properties) {
            PropertyInfrastructureSyncState state = syncState(property);
            Instant now = Instant.now();
            stats.poiProcessedCount++;

            try {
                PropertyInfrastructureSyncResult result =
                        propertyInfrastructureService.syncNearbyInfrastructures(property);
                recordSuccess(state, now, result);
                stats.recordSuccess(result);
                log.info(
                        "Synced property infrastructures from TMAP POI. trigger={}, propertyId={}, "
                                + "infrastructures={}, poiQuotaExceeded={}",
                        trigger,
                        property.getPropertyId(),
                        result.infrastructureCount(),
                        result.poiQuotaExceeded()
                );

                if (result.poiQuotaExceeded()) {
                    state.recordQuotaStopped(
                            now,
                            result.infrastructureCount(),
                            result.createdAccessibilityCount()
                    );
                    syncStateRepository.save(state);
                    poiQuotaService.markQuotaExhaustedNow();
                    stats.markPoiQuotaExceeded();
                    log.warn(
                            "Stopping TMAP POI infrastructure sync phase because POI quota appears exhausted. "
                                    + "Accessibility phase will still run from stored infrastructures. "
                                    + "trigger={}, propertyId={}, processed={}, remaining={}",
                            trigger,
                            property.getPropertyId(),
                            stats.poiProcessedCount,
                            properties.size() - stats.poiProcessedCount
                    );
                    return;
                }

                syncStateRepository.save(state);
            } catch (RuntimeException e) {
                stats.recordFailure();
                state.recordFailure(now);
                syncStateRepository.save(state);
                log.warn(
                        "TMAP POI infrastructure sync failed for propertyId={}, trigger={}: {}",
                        property.getPropertyId(),
                        trigger,
                        e.getMessage(),
                        e
                );
            }
        }
    }

    private void runAccessibilityPhase(String trigger, List<Property> properties, SyncRunStats stats) {
        log.info(
                "Starting infra accessibility sync phase. trigger={}, propertyCount={}",
                trigger,
                properties.size()
        );
        for (Property property : properties) {
            PropertyInfrastructureSyncState state = syncState(property);
            Instant now = Instant.now();
            stats.accessibilityProcessedCount++;

            try {
                PropertyInfrastructureSyncResult result =
                        propertyInfrastructureService.syncMissingAccessibilities(property);
                recordSuccess(state, now, result);
                stats.recordSuccess(result);
                log.info(
                        "Synced property infra accessibilities. trigger={}, propertyId={}, infrastructures={}, "
                                + "createdAccessibilities={}, walkingRouteQuotaExceeded={}",
                        trigger,
                        property.getPropertyId(),
                        result.infrastructureCount(),
                        result.createdAccessibilityCount(),
                        result.walkingRouteQuotaExceeded()
                );

                if (result.walkingRouteQuotaExceeded()) {
                    state.recordQuotaStopped(
                            now,
                            result.infrastructureCount(),
                            result.createdAccessibilityCount()
                    );
                    syncStateRepository.save(state);
                    stats.markWalkingRouteQuotaExceeded();
                    log.warn(
                            "Stopping infra accessibility sync phase because TMAP walking route quota appears "
                                    + "exhausted. trigger={}, propertyId={}, processed={}, remaining={}",
                            trigger,
                            property.getPropertyId(),
                            stats.accessibilityProcessedCount,
                            properties.size() - stats.accessibilityProcessedCount
                    );
                    return;
                }

                syncStateRepository.save(state);
            } catch (RuntimeException e) {
                stats.recordFailure();
                state.recordFailure(now);
                syncStateRepository.save(state);
                log.warn(
                        "Infra accessibility sync failed for propertyId={}, trigger={}: {}",
                        property.getPropertyId(),
                        trigger,
                        e.getMessage(),
                        e
                );
            }
        }
    }

    private void recordSuccess(
            PropertyInfrastructureSyncState state,
            Instant now,
            PropertyInfrastructureSyncResult result
    ) {
        state.recordMissingSync(
                now,
                result.infrastructureCount(),
                result.createdAccessibilityCount()
        );
    }

    private PropertyInfrastructureSyncState syncState(Property property) {
        return syncStateRepository.findById(property.getPropertyId())
                .orElseGet(() -> new PropertyInfrastructureSyncState(property.getPropertyId()));
    }

    private enum SyncRunStatus {
        COMPLETED,
        COMPLETED_WITH_FAILURES,
        QUOTA_LIMITED,
        SKIPPED_DISABLED
    }

    private static final class SyncRunStats {
        private SyncRunStatus status = SyncRunStatus.COMPLETED;
        private final int propertiesFound;
        private int poiProcessedCount;
        private int accessibilityProcessedCount;
        private int successCount;
        private int failureCount;
        private int totalInfrastructureCount;
        private int totalCreatedAccessibilityCount;
        private int totalRemovedAccessibilityCount;
        private boolean poiQuotaExceeded;
        private boolean walkingRouteQuotaExceeded;

        private SyncRunStats(int propertiesFound) {
            this.propertiesFound = propertiesFound;
        }

        private static SyncRunStats skipped(SyncRunStatus status) {
            SyncRunStats stats = new SyncRunStats(0);
            stats.status = status;
            return stats;
        }

        private void recordSuccess(PropertyInfrastructureSyncResult result) {
            successCount++;
            totalInfrastructureCount += Math.max(0, result.infrastructureCount());
            totalCreatedAccessibilityCount += Math.max(0, result.createdAccessibilityCount());
            totalRemovedAccessibilityCount += Math.max(0, result.removedAccessibilityCount());
        }

        private void recordFailure() {
            failureCount++;
            if (status == SyncRunStatus.COMPLETED) {
                status = SyncRunStatus.COMPLETED_WITH_FAILURES;
            }
        }

        private void markPoiQuotaExceeded() {
            poiQuotaExceeded = true;
            status = SyncRunStatus.QUOTA_LIMITED;
        }

        private void markWalkingRouteQuotaExceeded() {
            walkingRouteQuotaExceeded = true;
            status = SyncRunStatus.QUOTA_LIMITED;
        }
    }

    private long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }
}
