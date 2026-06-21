package com.rooming.domain.locations.service;

import com.rooming.domain.property.entity.Property;
import com.rooming.domain.property.repository.PropertyRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyRouteSyncServiceTest {

    private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
    private final TargetPlaceRouteService targetPlaceRouteService = mock(TargetPlaceRouteService.class);
    private final PropertyRouteSyncService propertyRouteSyncService = new PropertyRouteSyncService(
            propertyRepository,
            targetPlaceRouteService
    );

    @Test
    void storedPropertyIsLoadedBeforeGeneratingRoutes() {
        Property property = Property.builder()
                .propertyId(101L)
                .latitude(37.2961)
                .longitude(126.9718)
                .build();
        when(propertyRepository.findById(101L)).thenReturn(Optional.of(property));

        propertyRouteSyncService.storeMissingRoutesAsync(101L);

        verify(targetPlaceRouteService)
                .storeMissingRoutesToTargetPlacesWithinFiveKilometers(property);
    }

    @Test
    void missingPropertyDoesNotGenerateRoutes() {
        when(propertyRepository.findById(101L)).thenReturn(Optional.empty());

        propertyRouteSyncService.storeMissingRoutesAsync(101L);

        verifyNoInteractions(targetPlaceRouteService);
    }
}
