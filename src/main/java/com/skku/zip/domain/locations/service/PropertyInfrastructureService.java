package com.skku.zip.domain.locations.service;

import com.skku.zip.domain.locations.client.TmapClient;
import com.skku.zip.domain.locations.dto.TmapInfrastructureCandidate;
import com.skku.zip.domain.locations.entity.id.PropertyInfrastructureId;
import com.skku.zip.domain.locations.entity.model.InfraAccessibility;
import com.skku.zip.domain.locations.entity.model.Infrastructure;
import com.skku.zip.domain.locations.entity.type.INFRA_CATEGORY;
import com.skku.zip.domain.locations.repository.InfraAccessibilityRepository;
import com.skku.zip.domain.locations.repository.InfrastructureRepository;
import com.skku.zip.domain.property.entity.Property;
import com.skku.zip.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyInfrastructureService {

    private static final double INFRASTRUCTURE_RADIUS_KM = 1.0;
    private static final double DUPLICATE_LOCATION_TOLERANCE_KM = 0.03;
    private static final double METERS_PER_KILOMETER = 1000.0;
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int MAX_INFRASTRUCTURES_PER_PROPERTY = 50;
    private static final int MAX_INFRASTRUCTURES_PER_CATEGORY = 5;

    private final TmapClient tmapClient;
    private final PropertyRepository propertyRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final InfraAccessibilityRepository infraAccessibilityRepository;

    @Async
    public void storeInfrastructureAccessibilitiesAsync(Long propertyId) {
        if (propertyId == null) {
            return;
        }

        try {
            propertyRepository.findById(propertyId)
                    .ifPresentOrElse(
                            this::storeInfrastructureAccessibilities,
                            () -> log.warn("Skipping infrastructure processing because propertyId={} was not found", propertyId)
                    );
        } catch (RuntimeException e) {
            log.error("Infrastructure processing failed for propertyId={}: {}", propertyId, e.getMessage(), e);
        }
    }

    public List<Infrastructure> findNearbyInfrastructures(Property property) {
        validatePropertyLocation(property);

        List<Infrastructure> cachedInfrastructures = infrastructureRepository.findWithinMeters(
                property.getLatitude(),
                property.getLongitude(),
                INFRASTRUCTURE_RADIUS_KM * METERS_PER_KILOMETER,
                MAX_INFRASTRUCTURES_PER_PROPERTY
        );
        if (!cachedInfrastructures.isEmpty()) {
            log.info(
                    "Using {} cached infrastructures for propertyId={} within {}km",
                    cachedInfrastructures.size(),
                    property.getPropertyId(),
                    INFRASTRUCTURE_RADIUS_KM
            );
            return cachedInfrastructures;
        }

        List<Infrastructure> fetchedInfrastructures = tmapClient.findInfrastructureCandidates(
                        property.getLatitude(),
                        property.getLongitude(),
                        (int) INFRASTRUCTURE_RADIUS_KM
                )
                .stream()
                .map(this::toInfrastructure)
                .toList();

        List<Infrastructure> selectedInfrastructures = selectNearestByCategory(property, fetchedInfrastructures);
        log.info(
                "Fetched {} infrastructures from TMAP and selected {} for propertyId={}",
                fetchedInfrastructures.size(),
                selectedInfrastructures.size(),
                property.getPropertyId()
        );
        return selectedInfrastructures;
    }

    public List<InfraAccessibility> buildInfraAccessibilities(Property property, List<Infrastructure> infrastructures) {
        validatePropertyLocation(property);

        return infrastructures.stream()
                .map(infrastructure -> buildInfraAccessibility(property, infrastructure))
                .flatMap(Optional::stream)
                .toList();
    }

    public List<InfraAccessibility> storeInfrastructureAccessibilities(Property property) {
        List<Infrastructure> nearbyInfrastructureCandidates = findNearbyInfrastructures(property);
        log.info(
                "Found {} infrastructure candidates for propertyId={}",
                nearbyInfrastructureCandidates.size(),
                property.getPropertyId()
        );

        List<Infrastructure> infrastructures = nearbyInfrastructureCandidates.stream()
                .map(this::findOrSaveInfrastructure)
                .flatMap(Optional::stream)
                .toList();

        List<InfraAccessibility> savedAccessibilities = new ArrayList<>();
        int alreadyExistingCount = 0;
        int noRouteCount = 0;
        int failedCount = 0;

        for (Infrastructure infrastructure : infrastructures) {
            PropertyInfrastructureId id = new PropertyInfrastructureId(property.getPropertyId(), infrastructure.getId());
            if (infraAccessibilityRepository.existsById(id)) {
                alreadyExistingCount++;
                continue;
            }

            try {
                Optional<InfraAccessibility> accessibility = buildInfraAccessibility(property, infrastructure);
                if (accessibility.isEmpty()) {
                    noRouteCount++;
                    continue;
                }
                Optional<InfraAccessibility> saved = saveInfraAccessibility(accessibility.get());
                if (saved.isPresent()) {
                    savedAccessibilities.add(saved.get());
                } else {
                    failedCount++;
                }
            } catch (RuntimeException e) {
                failedCount++;
                log.warn(
                        "Failed to store infra accessibility for propertyId={}, infrastructureId={}: {}",
                        property.getPropertyId(),
                        infrastructure.getId(),
                        e.getMessage()
                );
            }
        }

        log.info(
                "Infrastructure accessibility processing completed for propertyId={}: saved={}, existing={}, noRoute={}, failed={}",
                property.getPropertyId(),
                savedAccessibilities.size(),
                alreadyExistingCount,
                noRouteCount,
                failedCount
        );
        return savedAccessibilities;
    }

    private Optional<Infrastructure> findOrSaveInfrastructure(Infrastructure infrastructure) {
        Optional<Infrastructure> existingInfrastructure = findExistingInfrastructure(infrastructure);
        if (existingInfrastructure.isPresent()) {
            return existingInfrastructure;
        }

        try {
            return Optional.of(infrastructureRepository.saveAndFlush(infrastructure));
        } catch (DataIntegrityViolationException e) {
            log.info(
                    "Infrastructure already exists while saving name={}, category={}. Reusing existing row if possible.",
                    infrastructure.getName(),
                    infrastructure.getCategory()
            );
            return findExistingInfrastructure(infrastructure);
        } catch (RuntimeException e) {
            log.warn(
                    "Failed to save infrastructure name={}, category={}: {}",
                    infrastructure.getName(),
                    infrastructure.getCategory(),
                    e.getMessage()
            );
            return Optional.empty();
        }
    }

    private Optional<Infrastructure> findExistingInfrastructure(Infrastructure infrastructure) {
        if (infrastructure.getAddress() != null) {
            Optional<Infrastructure> existingByAddress = infrastructureRepository.findByAddress(infrastructure.getAddress());
            if (existingByAddress.isPresent()) {
                return existingByAddress;
            }
        }

        return infrastructureRepository.findAllByNameAndCategory(infrastructure.getName(), infrastructure.getCategory())
                .stream()
                .filter(existing -> distanceKm(
                        existing.getLatitude(),
                        existing.getLongitude(),
                        infrastructure.getLatitude(),
                        infrastructure.getLongitude()
                ) <= DUPLICATE_LOCATION_TOLERANCE_KM)
                .min(Comparator.comparingDouble(existing -> distanceKm(
                        existing.getLatitude(),
                        existing.getLongitude(),
                        infrastructure.getLatitude(),
                        infrastructure.getLongitude()
                )));
    }

    private Optional<InfraAccessibility> saveInfraAccessibility(InfraAccessibility accessibility) {
        try {
            return Optional.of(infraAccessibilityRepository.saveAndFlush(accessibility));
        } catch (DataIntegrityViolationException e) {
            return infraAccessibilityRepository.findById(accessibility.getId());
        } catch (RuntimeException e) {
            log.warn(
                    "Failed to save infra accessibility propertyId={}, infrastructureId={}: {}",
                    accessibility.getId().getPropertyId(),
                    accessibility.getId().getInfrastructureId(),
                    e.getMessage()
            );
            return Optional.empty();
        }
    }

    private Optional<InfraAccessibility> buildInfraAccessibility(Property property, Infrastructure infrastructure) {
        if (infrastructure.getId() == null) {
            throw new IllegalArgumentException("Save Infrastructure before creating InfraAccessibility.");
        }

        return tmapClient.findWalkingRoute(
                        property.getLatitude(),
                        property.getLongitude(),
                        infrastructure.getLatitude(),
                        infrastructure.getLongitude()
                )
                .map(route -> new InfraAccessibility(property, infrastructure, route.duration(), route.path()));
    }

    private List<Infrastructure> selectNearestByCategory(Property property, List<Infrastructure> infrastructures) {
        Map<INFRA_CATEGORY, Integer> categoryCounts = new EnumMap<>(INFRA_CATEGORY.class);
        List<Infrastructure> selectedInfrastructures = new ArrayList<>();

        infrastructures.stream()
                .sorted(Comparator.comparingDouble(infrastructure -> distanceKm(
                        property.getLatitude(),
                        property.getLongitude(),
                        infrastructure.getLatitude(),
                        infrastructure.getLongitude()
                )))
                .forEach(infrastructure -> {
                    if (selectedInfrastructures.size() >= MAX_INFRASTRUCTURES_PER_PROPERTY) {
                        return;
                    }

                    INFRA_CATEGORY category = infrastructure.getCategory();
                    int categoryCount = categoryCounts.getOrDefault(category, 0);
                    if (categoryCount >= MAX_INFRASTRUCTURES_PER_CATEGORY) {
                        return;
                    }

                    categoryCounts.put(category, categoryCount + 1);
                    selectedInfrastructures.add(infrastructure);
                });

        return selectedInfrastructures;
    }

    private Infrastructure toInfrastructure(TmapInfrastructureCandidate candidate) {
        return new Infrastructure(
                candidate.name(),
                candidate.category(),
                candidate.latitude(),
                candidate.longitude(),
                candidate.address()
        );
    }

    private double distanceKm(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        double latitudeDelta = Math.toRadians(endLatitude - startLatitude);
        double longitudeDelta = Math.toRadians(endLongitude - startLongitude);
        double startLatitudeRadians = Math.toRadians(startLatitude);
        double endLatitudeRadians = Math.toRadians(endLatitude);

        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(startLatitudeRadians) * Math.cos(endLatitudeRadians)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private void validatePropertyLocation(Property property) {
        if (property == null || property.getPropertyId() == null) {
            throw new IllegalArgumentException("Property must not be null and must have an id.");
        }
        if (property.getLatitude() == null || property.getLongitude() == null) {
            throw new IllegalArgumentException("Property latitude and longitude must not be null.");
        }
    }
}
