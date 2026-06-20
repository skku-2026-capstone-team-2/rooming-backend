package com.rooming.domain.locations.service;

import com.rooming.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyRouteSyncService {

    private final PropertyRepository propertyRepository;
    private final TargetPlaceRouteService targetPlaceRouteService;

    @Async
    public void storeMissingRoutesAsync(Long propertyId) {
        if (propertyId == null) {
            return;
        }

        try {
            propertyRepository.findById(propertyId)
                    .ifPresentOrElse(
                            targetPlaceRouteService::storeMissingRoutesToTargetPlacesWithinFiveKilometers,
                            () -> log.warn(
                                    "Skipping target-place route processing because propertyId={} was not found",
                                    propertyId
                            )
                    );
        } catch (RuntimeException e) {
            log.error(
                    "Target-place route processing failed for propertyId={}: {}",
                    propertyId,
                    e.getMessage(),
                    e
            );
        }
    }
}
