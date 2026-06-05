package com.rooming.domain.locations.service;

import com.rooming.domain.locations.dto.PropertyInfrastructureSyncResult;
import com.rooming.domain.locations.entity.model.PropertyInfrastructureSyncState;
import com.rooming.domain.locations.repository.InfraAccessibilityRepository;
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
    private final InfraAccessibilityRepository infraAccessibilityRepository =
            mock(InfraAccessibilityRepository.class);
    private final PropertyInfrastructureService propertyInfrastructureService =
            mock(PropertyInfrastructureService.class);
    private final PropertyInfrastructureSyncStateRepository syncStateRepository =
            mock(PropertyInfrastructureSyncStateRepository.class);
    private final TmapPoiSyncQuotaService quotaService = mock(TmapPoiSyncQuotaService.class);
    private final DailyPropertyInfrastructureSyncScheduler scheduler =
            new DailyPropertyInfrastructureSyncScheduler(
                    propertyRepository,
                    infraAccessibilityRepository,
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
    void processesMissingPropertiesBeforeRefreshingExistingProperties() {
        Property missingProperty = property(101L);
        Property existingProperty = property(202L);
        when(quotaService.isQuotaExhaustedToday()).thenReturn(false);
        when(propertyRepository.findPropertiesForDailyInfrastructureSync())
                .thenReturn(List.of(missingProperty, existingProperty));
        when(syncStateRepository.findById(101L)).thenReturn(Optional.empty());
        when(syncStateRepository.findById(202L)).thenReturn(Optional.empty());
        when(infraAccessibilityRepository.existsByPropertyPropertyId(101L)).thenReturn(false);
        when(infraAccessibilityRepository.existsByPropertyPropertyId(202L)).thenReturn(true);
        when(propertyInfrastructureService.syncMissingInfrastructure(missingProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 2, 0, false));
        when(propertyInfrastructureService.refreshInfrastructureSelection(existingProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 1, 1, false));

        scheduler.syncAllPropertyInfrastructures();

        InOrder order = inOrder(propertyInfrastructureService);
        order.verify(propertyInfrastructureService).syncMissingInfrastructure(missingProperty);
        order.verify(propertyInfrastructureService).refreshInfrastructureSelection(existingProperty);
        verify(quotaService, never()).markQuotaExhaustedNow();
    }

    @Test
    void stopsAfterQuotaExceededAndStoresCurrentPropertyState() {
        Property firstProperty = property(101L);
        Property secondProperty = property(202L);
        when(quotaService.isQuotaExhaustedToday()).thenReturn(false);
        when(propertyRepository.findPropertiesForDailyInfrastructureSync())
                .thenReturn(List.of(firstProperty, secondProperty));
        when(syncStateRepository.findById(101L)).thenReturn(Optional.empty());
        when(infraAccessibilityRepository.existsByPropertyPropertyId(101L)).thenReturn(false);
        when(propertyInfrastructureService.syncMissingInfrastructure(firstProperty))
                .thenReturn(new PropertyInfrastructureSyncResult(2, 2, 0, true));

        scheduler.syncAllPropertyInfrastructures();

        verify(propertyInfrastructureService).syncMissingInfrastructure(firstProperty);
        verify(propertyInfrastructureService, never()).refreshInfrastructureSelection(secondProperty);
        verify(propertyInfrastructureService, never()).syncMissingInfrastructure(secondProperty);
        verify(quotaService).markQuotaExhaustedNow();

        ArgumentCaptor<PropertyInfrastructureSyncState> stateCaptor =
                ArgumentCaptor.forClass(PropertyInfrastructureSyncState.class);
        verify(syncStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getPropertyId()).isEqualTo(101L);
        assertThat(stateCaptor.getValue().getLastInfrastructureCount()).isEqualTo(2);
        assertThat(stateCaptor.getValue().getLastCreatedAccessibilityCount()).isEqualTo(2);
    }

    private Property property(Long propertyId) {
        return Property.builder()
                .propertyId(propertyId)
                .latitude(37.2910)
                .longitude(126.9710)
                .build();
    }
}
