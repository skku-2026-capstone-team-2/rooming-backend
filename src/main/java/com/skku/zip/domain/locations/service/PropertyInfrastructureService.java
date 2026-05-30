package com.skku.zip.domain.locations.service;

import com.skku.zip.domain.locations.client.TmapClient;
import com.skku.zip.domain.locations.dto.TmapInfrastructureCandidate;
import com.skku.zip.domain.locations.entity.id.PropertyInfrastructureId;
import com.skku.zip.domain.locations.entity.model.InfraAccessibility;
import com.skku.zip.domain.locations.entity.model.Infrastructure;
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
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyInfrastructureService {

    private static final double INFRASTRUCTURE_RADIUS_KM = 1.0;
    private static final double SAME_LOCATION_TOLERANCE_METERS = 0.01;

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

        return tmapClient.findInfrastructureCandidates(
                        property.getLatitude(),
                        property.getLongitude(),
                        (int) INFRASTRUCTURE_RADIUS_KM
                )
                .stream()
                .map(this::toInfrastructure)
                .toList();
    }

    public List<InfraAccessibility> storeInfrastructureAccessibilities(Property property) {
        List<Infrastructure> nearbyInfrastructureCandidates = findNearbyInfrastructures(property);

        List<Infrastructure> infrastructures = nearbyInfrastructureCandidates.stream()
                .map(this::findOrSaveInfrastructure)
                .flatMap(Optional::stream)
                .toList();

        List<InfraAccessibility> savedAccessibilities = new ArrayList<>();

        for (Infrastructure infrastructure : infrastructures) {
            PropertyInfrastructureId id = new PropertyInfrastructureId(property.getPropertyId(), infrastructure.getId());
            if (infraAccessibilityRepository.existsById(id)) {
                continue;
            }

            try {
                Optional<InfraAccessibility> accessibility = buildInfraAccessibility(property, infrastructure);
                if (accessibility.isEmpty()) {
                    log.warn(
                            "Skipping infra accessibility because TMAP walking route was unavailable for propertyId={}, infrastructureId={}",
                            property.getPropertyId(),
                            infrastructure.getId()
                    );
                    continue;
                }
                Optional<InfraAccessibility> saved = saveInfraAccessibility(accessibility.get());
                if (saved.isPresent()) {
                    savedAccessibilities.add(saved.get());
                }
            } catch (RuntimeException e) {
                log.warn(
                        "Failed during infra accessibility processing for propertyId={}, infrastructureId={}: {}",
                        property.getPropertyId(),
                        infrastructure.getId(),
                        e.getMessage()
                );
            }
        }

        return savedAccessibilities;
    }

    private Optional<Infrastructure> findOrSaveInfrastructure(Infrastructure infrastructure) {
        try {
            return Optional.of(infrastructureRepository.saveAndFlush(infrastructure));
        } catch (DataIntegrityViolationException e) {
            Optional<Infrastructure> existingInfrastructure = findExistingInfrastructure(infrastructure);
            if (existingInfrastructure.isEmpty()) {
                log.warn(
                        "Failed to resolve existing infrastructure after duplicate insert for name={}, category={}",
                        infrastructure.getName(),
                        infrastructure.getCategory()
                );
            }
            return existingInfrastructure;
        } catch (RuntimeException e) {
            log.warn(
                    "Failed during infrastructure save for name={}, category={}: {}",
                    infrastructure.getName(),
                    infrastructure.getCategory(),
                    e.getMessage()
            );
            return Optional.empty();
        }
    }

    private Optional<Infrastructure> findExistingInfrastructure(Infrastructure infrastructure) {
        Optional<Infrastructure> existingByLocation = infrastructureRepository.findFirstByLocationWithinMeters(
                infrastructure.getLatitude(),
                infrastructure.getLongitude(),
                SAME_LOCATION_TOLERANCE_METERS
        );
        if (existingByLocation.isPresent()) {
            return existingByLocation;
        }

        if (infrastructure.getAddress() != null) {
            Optional<Infrastructure> existingByAddress = infrastructureRepository.findByAddress(infrastructure.getAddress());
            if (existingByAddress.isPresent()) {
                return existingByAddress;
            }
        }

        return Optional.empty();
    }

    private Optional<InfraAccessibility> saveInfraAccessibility(InfraAccessibility accessibility) {
        try {
            return Optional.of(infraAccessibilityRepository.saveAndFlush(accessibility));
        } catch (DataIntegrityViolationException e) {
            Optional<InfraAccessibility> existingAccessibility = infraAccessibilityRepository.findById(accessibility.getId());
            if (existingAccessibility.isEmpty()) {
                log.warn(
                        "Failed to resolve existing infra accessibility after duplicate insert for propertyId={}, infrastructureId={}",
                        accessibility.getId().getPropertyId(),
                        accessibility.getId().getInfrastructureId()
                );
            }
            return existingAccessibility;
        } catch (RuntimeException e) {
            log.warn(
                    "Failed during infra accessibility save for propertyId={}, infrastructureId={}: {}",
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

    private Infrastructure toInfrastructure(TmapInfrastructureCandidate candidate) {
        return new Infrastructure(
                candidate.name(),
                candidate.category(),
                candidate.latitude(),
                candidate.longitude(),
                candidate.address()
        );
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
