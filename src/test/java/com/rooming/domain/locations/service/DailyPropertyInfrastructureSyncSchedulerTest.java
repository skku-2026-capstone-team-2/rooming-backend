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
import org.springframework.boot.context.event.ApplicationReadyEvent;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyPropertyInfrastructureSyncSchedulerTest {

    private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
    private final PropertyInfrastructureService propertyInfrastructureService =
            mock(PropertyInfrastructureService.class);
    private final PropertyInfrastructureSyncStateRepository syncStateRepository =
            mock(PropertyInfrastructureSyncStateRepository.class);
    private final TmapPoiSyncQuotaService poiQuotaService = mock(TmapPoiSyncQuotaService.class);
    private final DailyPropertyInfrastructureSyncScheduler scheduler =
            new DailyPropertyInfrastructureSyncScheduler(
                    propertyRepository,
                    propertyInfrastructureService,
                    syncStateRepository,
                    poiQuotaService
            );

    @BeforeEach
    void configureScheduler() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
    }

    @Test
    void schedulerRunsOnStartupAndDailyCron() throws NoSuchMethodException {
        Method scheduledMethod = DailyPropertyInfrastructureSyncScheduler.class
                .getMethod("syncAllPropertyInfrastructures");
        Method startupMethod = DailyPropertyInfrastructureSyncScheduler.class
                .getMethod("syncAllPropertyInfrastructuresOnStartup");

        assertThat(scheduledMethod.getAnnotation(Scheduled.class)).isNotNull();
        EventListener startupListener = startupMethod.getAnnotation(EventListener.class);
        assertThat(startupListener).isNotNull();
        assertThat(startupListener.value()).contains(ApplicationReadyEvent.class);
    }

    @Test
    void runsPoiPhaseBeforeAccessibilityPhase() {
        Property firstProperty = property(101L);
        Property secondProperty = property(202L);
        when(poiQuotaService.isQuotaExhaustedToday()).thenReturn(false);
        when(propertyRepository.findPropertiesForDailyInfrastructureSync())
                .thenReturn(List.of(firstProperty, secondProperty));
        when(syncStateRepository.findById(101L)).thenReturn(Optional.empty());
        when(syncStateRepository.findById(202L)).thenReturn(Optional.empty());
        when(propertyInfrastructureService.syncNearbyInfrastructures(firstProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 0, 0, false, false));
        when(propertyInfrastructureService.syncNearbyInfrastructures(secondProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 0, 0, false, false));
        when(propertyInfrastructureService.syncMissingAccessibilities(firstProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 1, 0, false, false));
        when(propertyInfrastructureService.syncMissingAccessibilities(secondProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 1, 0, false, false));

        scheduler.syncAllPropertyInfrastructures();

        InOrder order = inOrder(propertyInfrastructureService);
        order.verify(propertyInfrastructureService).syncNearbyInfrastructures(firstProperty);
        order.verify(propertyInfrastructureService).syncNearbyInfrastructures(secondProperty);
        order.verify(propertyInfrastructureService).syncMissingAccessibilities(firstProperty);
        order.verify(propertyInfrastructureService).syncMissingAccessibilities(secondProperty);
        verify(poiQuotaService, never()).markQuotaExhaustedNow();
    }

    @Test
    void startsAccessibilityPhaseAfterPoiQuotaExceeded() {
        Property firstProperty = property(101L);
        Property secondProperty = property(202L);
        when(poiQuotaService.isQuotaExhaustedToday()).thenReturn(false);
        when(propertyRepository.findPropertiesForDailyInfrastructureSync())
                .thenReturn(List.of(firstProperty, secondProperty));
        when(syncStateRepository.findById(101L)).thenReturn(Optional.empty());
        when(syncStateRepository.findById(202L)).thenReturn(Optional.empty());
        when(propertyInfrastructureService.syncNearbyInfrastructures(firstProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 0, 0, true, false));
        when(propertyInfrastructureService.syncMissingAccessibilities(firstProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 1, 0, false, false));
        when(propertyInfrastructureService.syncMissingAccessibilities(secondProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 1, 0, false, false));

        scheduler.syncAllPropertyInfrastructures();

        verify(propertyInfrastructureService).syncNearbyInfrastructures(firstProperty);
        verify(propertyInfrastructureService, never()).syncNearbyInfrastructures(secondProperty);
        verify(propertyInfrastructureService).syncMissingAccessibilities(firstProperty);
        verify(propertyInfrastructureService).syncMissingAccessibilities(secondProperty);
        verify(poiQuotaService).markQuotaExhaustedNow();

        ArgumentCaptor<PropertyInfrastructureSyncState> stateCaptor =
                ArgumentCaptor.forClass(PropertyInfrastructureSyncState.class);
        verify(syncStateRepository, times(3)).save(stateCaptor.capture());
        assertThat(stateCaptor.getAllValues())
                .extracting(PropertyInfrastructureSyncState::getPropertyId)
                .containsExactly(101L, 101L, 202L);
    }

    @Test
    void skipsPoiPhaseButRunsAccessibilityPhaseWhenPoiQuotaAlreadyExhaustedToday() {
        Property firstProperty = property(101L);
        Property secondProperty = property(202L);
        when(poiQuotaService.isQuotaExhaustedToday()).thenReturn(true);
        when(propertyRepository.findPropertiesForDailyInfrastructureSync())
                .thenReturn(List.of(firstProperty, secondProperty));
        when(syncStateRepository.findById(101L)).thenReturn(Optional.empty());
        when(syncStateRepository.findById(202L)).thenReturn(Optional.empty());
        when(propertyInfrastructureService.syncMissingAccessibilities(firstProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 1, 0, false, false));
        when(propertyInfrastructureService.syncMissingAccessibilities(secondProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 1, 0, false, false));

        scheduler.syncAllPropertyInfrastructures();

        verify(propertyInfrastructureService, never()).syncNearbyInfrastructures(firstProperty);
        verify(propertyInfrastructureService, never()).syncNearbyInfrastructures(secondProperty);
        verify(propertyInfrastructureService).syncMissingAccessibilities(firstProperty);
        verify(propertyInfrastructureService).syncMissingAccessibilities(secondProperty);
        verify(poiQuotaService, never()).markQuotaExhaustedNow();
    }

    @Test
    void stopsAccessibilityPhaseAfterWalkingRouteQuotaExceeded() {
        Property firstProperty = property(101L);
        Property secondProperty = property(202L);
        when(poiQuotaService.isQuotaExhaustedToday()).thenReturn(false);
        when(propertyRepository.findPropertiesForDailyInfrastructureSync())
                .thenReturn(List.of(firstProperty, secondProperty));
        when(syncStateRepository.findById(101L)).thenReturn(Optional.empty());
        when(syncStateRepository.findById(202L)).thenReturn(Optional.empty());
        when(propertyInfrastructureService.syncNearbyInfrastructures(firstProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 0, 0, false, false));
        when(propertyInfrastructureService.syncNearbyInfrastructures(secondProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 0, 0, false, false));
        when(propertyInfrastructureService.syncMissingAccessibilities(firstProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 1, 0, false, true));

        scheduler.syncAllPropertyInfrastructures();

        verify(propertyInfrastructureService).syncNearbyInfrastructures(firstProperty);
        verify(propertyInfrastructureService).syncNearbyInfrastructures(secondProperty);
        verify(propertyInfrastructureService).syncMissingAccessibilities(firstProperty);
        verify(propertyInfrastructureService, never()).syncMissingAccessibilities(secondProperty);
        verify(poiQuotaService, never()).markQuotaExhaustedNow();
    }

    private Property property(Long propertyId) {
        return Property.builder()
                .propertyId(propertyId)
                .latitude(37.2910)
                .longitude(126.9710)
                .nearbyInfrastructuresFetched(false)
                .infraAccessibilitiesFetched(false)
                .build();
    }
}
