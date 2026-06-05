package com.rooming.domain.locations.service;

import com.rooming.domain.locations.client.TmapClient;
import com.rooming.domain.locations.dto.PropertyInfrastructureSyncResult;
import com.rooming.domain.locations.dto.TmapInfrastructureCandidate;
import com.rooming.domain.locations.dto.TmapInfrastructureSearchResult;
import com.rooming.domain.locations.entity.id.PropertyInfrastructureId;
import com.rooming.domain.locations.entity.model.InfraAccessibility;
import com.rooming.domain.locations.entity.model.Infrastructure;
import com.rooming.domain.locations.entity.type.INFRA_CATEGORY;
import com.rooming.domain.locations.repository.InfraAccessibilityRepository;
import com.rooming.domain.locations.repository.InfrastructureRepository;
import com.rooming.domain.property.entity.Property;
import com.rooming.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyInfrastructureService {

    private static final int INFRASTRUCTURE_RADIUS_KM = 1;
    private static final int DEFAULT_INFRASTRUCTURE_LIMIT_PER_CATEGORY = 2;
    private static final double SAME_LOCATION_TOLERANCE_METERS = 0.01;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

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
                        INFRASTRUCTURE_RADIUS_KM
                )
                .stream()
                .map(this::toInfrastructure)
                .toList();
    }

    @Transactional
    public List<InfraAccessibility> storeInfrastructureAccessibilities(Property property) {
        return syncClosestInfrastructures(property, false, DEFAULT_INFRASTRUCTURE_LIMIT_PER_CATEGORY)
                .createdAccessibilities();
    }

    @Transactional
    public PropertyInfrastructureSyncResult syncMissingInfrastructure(Property property) {
        SyncOperation operation = syncClosestInfrastructures(
                property,
                false,
                DEFAULT_INFRASTRUCTURE_LIMIT_PER_CATEGORY
        );
        return operation.toResult();
    }

    @Transactional
    public PropertyInfrastructureSyncResult refreshInfrastructureSelection(Property property) {
        SyncOperation operation = syncClosestInfrastructures(
                property,
                true,
                DEFAULT_INFRASTRUCTURE_LIMIT_PER_CATEGORY
        );
        return operation.toResult();
    }

    private SyncOperation syncClosestInfrastructures(
            Property property,
            boolean reconcileExisting,
            int infrastructureLimitPerCategory
    ) {
        validatePropertyLocation(property);

        TmapInfrastructureSearchResult searchResult = tmapClient.findInfrastructureCandidatesWithQuotaStatus(
                property.getLatitude(),
                property.getLongitude(),
                INFRASTRUCTURE_RADIUS_KM
        );
        List<Infrastructure> storedInfrastructures = storeInfrastructureCandidates(searchResult.candidates());
        List<Infrastructure> selectedInfrastructures = selectClosestPerCategory(
                property,
                storedInfrastructures,
                infrastructureLimitPerCategory
        );

        List<InfraAccessibility> createdAccessibilities = storeMissingAccessibilities(
                property,
                selectedInfrastructures
        );
        int removedAccessibilityCount = reconcileExisting
                ? removeObsoleteAccessibilities(property, selectedInfrastructures, searchResult.completedCategories())
                : 0;

        return new SyncOperation(
                selectedInfrastructures.size(),
                createdAccessibilities,
                removedAccessibilityCount,
                searchResult.quotaExceeded()
        );
    }

    private List<Infrastructure> storeInfrastructureCandidates(List<TmapInfrastructureCandidate> candidates) {
        return candidates.stream()
                .map(this::toInfrastructure)
                .map(this::findOrSaveInfrastructure)
                .flatMap(Optional::stream)
                .toList();
    }

    private List<Infrastructure> selectClosestPerCategory(
            Property property,
            List<Infrastructure> infrastructures,
            int infrastructureLimitPerCategory
    ) {
        return limitPerCategory(
                infrastructures.stream()
                        .sorted(Comparator.comparingDouble(infrastructure -> distanceMeters(property, infrastructure)))
                        .toList(),
                infrastructureLimitPerCategory
        );
    }

    private List<InfraAccessibility> storeMissingAccessibilities(
            Property property,
            List<Infrastructure> infrastructures
    ) {
        Map<Long, Infrastructure> infrastructuresById = new LinkedHashMap<>();
        for (Infrastructure infrastructure : infrastructures) {
            if (infrastructure != null && infrastructure.getId() != null) {
                infrastructuresById.putIfAbsent(infrastructure.getId(), infrastructure);
            }
        }

        List<InfraAccessibility> savedAccessibilities = new ArrayList<>();
        for (Infrastructure infrastructure : infrastructuresById.values()) {
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
                saved.ifPresent(savedAccessibilities::add);
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

    private int removeObsoleteAccessibilities(
            Property property,
            List<Infrastructure> selectedInfrastructures,
            Set<INFRA_CATEGORY> completedCategories
    ) {
        if (completedCategories == null || completedCategories.isEmpty()) {
            return 0;
        }

        Map<INFRA_CATEGORY, Set<Long>> selectedIdsByCategory = selectedInfrastructures.stream()
                .filter(infrastructure -> infrastructure.getId() != null && infrastructure.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Infrastructure::getCategory,
                        () -> new EnumMap<>(INFRA_CATEGORY.class),
                        Collectors.mapping(Infrastructure::getId, Collectors.toCollection(LinkedHashSet::new))
                ));

        List<InfraAccessibility> obsoleteAccessibilities = infraAccessibilityRepository
                .findAllByPropertyPropertyId(property.getPropertyId())
                .stream()
                .filter(accessibility -> isObsolete(accessibility, selectedIdsByCategory, completedCategories))
                .toList();
        if (obsoleteAccessibilities.isEmpty()) {
            return 0;
        }

        List<Long> obsoleteInfrastructureIds = obsoleteAccessibilities.stream()
                .map(accessibility -> accessibility.getId().getInfrastructureId())
                .distinct()
                .toList();
        infraAccessibilityRepository.deleteAll(obsoleteAccessibilities);
        infraAccessibilityRepository.flush();
        deleteOrphanInfrastructures(obsoleteInfrastructureIds);

        return obsoleteAccessibilities.size();
    }

    private boolean isObsolete(
            InfraAccessibility accessibility,
            Map<INFRA_CATEGORY, Set<Long>> selectedIdsByCategory,
            Set<INFRA_CATEGORY> completedCategories
    ) {
        Infrastructure infrastructure = accessibility.getInfrastructure();
        if (infrastructure == null || infrastructure.getCategory() == null) {
            return false;
        }

        INFRA_CATEGORY category = infrastructure.getCategory();
        if (!completedCategories.contains(category)) {
            return false;
        }

        Set<Long> selectedIds = selectedIdsByCategory.getOrDefault(category, Set.of());
        return !selectedIds.contains(infrastructure.getId());
    }

    private void deleteOrphanInfrastructures(List<Long> infrastructureIds) {
        for (Long infrastructureId : infrastructureIds) {
            if (infraAccessibilityRepository.countByIdInfrastructureId(infrastructureId) > 0) {
                continue;
            }
            try {
                infrastructureRepository.deleteById(infrastructureId);
            } catch (RuntimeException e) {
                log.warn(
                        "Skipping orphan infrastructure delete for infrastructureId={}: {}",
                        infrastructureId,
                        e.getMessage()
                );
            }
        }
    }

    private List<Infrastructure> limitPerCategory(
            List<Infrastructure> infrastructures,
            int infrastructureLimitPerCategory
    ) {
        int limit = limitPerCategory(infrastructureLimitPerCategory);
        Map<INFRA_CATEGORY, Integer> countsByCategory = new EnumMap<>(INFRA_CATEGORY.class);
        List<Infrastructure> selected = new ArrayList<>();
        for (Infrastructure infrastructure : infrastructures) {
            INFRA_CATEGORY category = infrastructure.getCategory();
            if (category == null) {
                continue;
            }
            int count = countsByCategory.getOrDefault(category, 0);
            if (count >= limit) {
                continue;
            }
            selected.add(infrastructure);
            countsByCategory.put(category, count + 1);
        }
        return selected;
    }

    private int limitPerCategory(int infrastructureLimitPerCategory) {
        return infrastructureLimitPerCategory <= 0
                ? DEFAULT_INFRASTRUCTURE_LIMIT_PER_CATEGORY
                : infrastructureLimitPerCategory;
    }

    private double distanceMeters(Property property, Infrastructure infrastructure) {
        double startLatitude = property.getLatitude();
        double startLongitude = property.getLongitude();
        double endLatitude = infrastructure.getLatitude();
        double endLongitude = infrastructure.getLongitude();
        double startLatRadians = Math.toRadians(startLatitude);
        double endLatRadians = Math.toRadians(endLatitude);
        double deltaLatRadians = Math.toRadians(endLatitude - startLatitude);
        double deltaLonRadians = Math.toRadians(endLongitude - startLongitude);

        double haversine = Math.sin(deltaLatRadians / 2) * Math.sin(deltaLatRadians / 2)
                + Math.cos(startLatRadians) * Math.cos(endLatRadians)
                * Math.sin(deltaLonRadians / 2) * Math.sin(deltaLonRadians / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
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

    private record SyncOperation(
            int infrastructureCount,
            List<InfraAccessibility> createdAccessibilities,
            int removedAccessibilityCount,
            boolean quotaExceeded
    ) {
        private PropertyInfrastructureSyncResult toResult() {
            return new PropertyInfrastructureSyncResult(
                    infrastructureCount,
                    createdAccessibilities.size(),
                    removedAccessibilityCount,
                    quotaExceeded
            );
        }
    }
}
