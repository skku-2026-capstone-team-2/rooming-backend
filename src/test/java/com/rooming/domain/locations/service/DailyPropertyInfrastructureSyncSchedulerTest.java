package com.rooming.domain.locations.service;

import com.rooming.domain.locations.dto.PropertyInfrastructureSyncResult;
import com.rooming.domain.locations.entity.model.PropertyInfrastructureSyncState;
import com.rooming.domain.locations.repository.PropertyInfrastructureSyncStateRepository;
import com.rooming.domain.property.entity.Property;
import com.rooming.domain.property.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyPropertyInfrastructureSyncSchedulerTest {

    private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
    private final PropertyInfrastructureService propertyInfrastructureService =
            mock(PropertyInfrastructureService.class);
    private final PropertyInfrastructureSyncStateRepository syncStateRepository =
            mock(PropertyInfrastructureSyncStateRepository.class);
    private final TmapPoiSyncQuotaService quotaService = mock(TmapPoiSyncQuotaService.class);
    private final DailyPropertyInfrastructureSyncScheduler scheduler =
            new DailyPropertyInfrastructureSyncScheduler(
                    propertyRepository,
                    propertyInfrastructureService,
                    syncStateRepository,
                    quotaService
            );

    @BeforeEach
    void configureScheduler() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
    }

    @Test
    void schedulerRunsOnStartupAndDailyCron() throws NoSuchMethodException {
        Method method = DailyPropertyInfrastructureSyncScheduler.class.getMethod("syncAllPropertyInfrastructures");

        assertThat(method.getAnnotation(EventListener.class)).isNotNull();
        assertThat(method.getAnnotation(Scheduled.class)).isNotNull();
    }

    @Test
    void skipsDailySyncWhenPoiQuotaIsAlreadyExhaustedToday() {
        when(quotaService.isQuotaExhaustedToday()).thenReturn(true);

        scheduler.syncAllPropertyInfrastructures();

        verify(propertyRepository, never()).findPropertiesForDailyInfrastructureSync();
    }

    @Test
    void processesNearbyFirstThenAccessibilitiesThenRefresh() {
        Property missingInfrastructureProperty = property(101L, false, false);
        Property missingAccessibilityProperty = property(202L, true, false);
        Property refreshProperty = property(303L, true, true);
        when(quotaService.isQuotaExhaustedToday()).thenReturn(false);
        when(propertyRepository.findPropertiesForDailyInfrastructureSync())
                .thenReturn(List.of(missingInfrastructureProperty, missingAccessibilityProperty, refreshProperty));
        when(syncStateRepository.findById(101L)).thenReturn(Optional.empty());
        when(syncStateRepository.findById(202L)).thenReturn(Optional.empty());
        when(syncStateRepository.findById(303L)).thenReturn(Optional.empty());
        when(propertyInfrastructureService.syncNearbyInfrastructures(missingInfrastructureProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 0, 0, false));
        when(propertyInfrastructureService.syncMissingAccessibilities(missingAccessibilityProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 1, 1, false));
        when(propertyInfrastructureService.refreshInfrastructureSelection(refreshProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 1, 1, false));

        scheduler.syncAllPropertyInfrastructures();

        InOrder order = inOrder(propertyInfrastructureService);
        order.verify(propertyInfrastructureService).syncNearbyInfrastructures(missingInfrastructureProperty);
        order.verify(propertyInfrastructureService).syncMissingAccessibilities(missingAccessibilityProperty);
        order.verify(propertyInfrastructureService).refreshInfrastructureSelection(refreshProperty);
        verify(quotaService, never()).markQuotaExhaustedNow();
    }

    @Test
    void stopsAfterQuotaExceededAndStoresCurrentPropertyState() {
        Property firstProperty = property(101L, false, false);
        Property secondProperty = property(202L, true, false);
        when(quotaService.isQuotaExhaustedToday()).thenReturn(false);
        when(propertyRepository.findPropertiesForDailyInfrastructureSync())
                .thenReturn(List.of(firstProperty, secondProperty));
        when(syncStateRepository.findById(101L)).thenReturn(Optional.empty());
        when(propertyInfrastructureService.syncNearbyInfrastructures(firstProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 0, 0, true));

        scheduler.syncAllPropertyInfrastructures();

        verify(propertyInfrastructureService).syncNearbyInfrastructures(firstProperty);
        verify(propertyInfrastructureService, never()).refreshInfrastructureSelection(secondProperty);
        verify(propertyInfrastructureService, never()).syncMissingAccessibilities(secondProperty);
        verify(quotaService).markQuotaExhaustedNow();

        ArgumentCaptor<PropertyInfrastructureSyncState> stateCaptor =
                ArgumentCaptor.forClass(PropertyInfrastructureSyncState.class);
        verify(syncStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getPropertyId()).isEqualTo(101L);
        assertThat(stateCaptor.getValue().getLastInfrastructureCount()).isEqualTo(2);
        assertThat(stateCaptor.getValue().getLastCreatedAccessibilityCount()).isZero();
    }

    private Property property(Long propertyId) {
        return property(propertyId, false, false);
    }

    private Property property(Long propertyId, boolean nearbyFetched, boolean accessibilityFetched) {
        return Property.builder()
                .propertyId(propertyId)
                .latitude(37.2910)
                .longitude(126.9710)
                .nearbyInfrastructuresFetched(nearbyFetched)
                .infraAccessibilitiesFetched(accessibilityFetched)
                .build();
    }
}
