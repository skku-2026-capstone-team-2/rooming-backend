package com.skku.zip.domain.locations.service;

import com.skku.zip.domain.locations.client.TmapClient;
import com.skku.zip.domain.locations.dto.TmapInfrastructureCandidate;
import com.skku.zip.domain.locations.entity.id.PropertyInfrastructureId;
import com.skku.zip.domain.locations.entity.model.InfraAccessibility;
import com.skku.zip.domain.locations.entity.model.Infrastructure;
import com.skku.zip.domain.locations.repository.InfraAccessibilityRepository;
import com.skku.zip.domain.locations.repository.InfrastructureRepository;
import com.skku.zip.domain.property.entity.Property;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PropertyInfrastructureService {

    private final TmapClient tmapClient;
    private final InfrastructureRepository infrastructureRepository;
    private final InfraAccessibilityRepository infraAccessibilityRepository;

    public List<Infrastructure> findInfrastructuresWithinTwoKilometers(Property property) {
        validatePropertyLocation(property);

        return tmapClient.findInfrastructureCandidates(
                        property.getLatitude(),
                        property.getLongitude(),
                        2
                )
                .stream()
                .map(this::toInfrastructure)
                .toList();
    }

    public List<InfraAccessibility> buildInfraAccessibilities(Property property, List<Infrastructure> infrastructures) {
        validatePropertyLocation(property);

        return infrastructures.stream()
                .map(infrastructure -> buildInfraAccessibility(property, infrastructure))
                .flatMap(Optional::stream)
                .toList();
    }

    @Transactional
    public List<InfraAccessibility> storeInfrastructureAccessibilities(Property property) {
        List<Infrastructure> infrastructures = findInfrastructuresWithinTwoKilometers(property).stream()
                .map(this::findOrSaveInfrastructure)
                .toList();

        List<InfraAccessibility> accessibilities = infrastructures.stream()
                .filter(infrastructure -> !infraAccessibilityRepository.existsById(
                        new PropertyInfrastructureId(property.getPropertyId(), infrastructure.getId())
                ))
                .map(infrastructure -> buildInfraAccessibility(property, infrastructure))
                .flatMap(Optional::stream)
                .toList();

        return infraAccessibilityRepository.saveAll(accessibilities);
    }

    private Infrastructure findOrSaveInfrastructure(Infrastructure infrastructure) {
        return infrastructureRepository.findAllByNameAndCategory(
                        infrastructure.getName(),
                        infrastructure.getCategory()
                )
                .stream()
                .filter(candidate -> sameInfrastructure(candidate, infrastructure))
                .findFirst()
                .orElseGet(() -> infrastructureRepository.save(infrastructure));
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

    private boolean sameInfrastructure(Infrastructure left, Infrastructure right) {
        return sameCoordinate(left.getLatitude(), right.getLatitude())
                && sameCoordinate(left.getLongitude(), right.getLongitude())
                && sameAddress(left, right);
    }

    private boolean sameCoordinate(double left, double right) {
        return Math.round(left * 1_000_000) == Math.round(right * 1_000_000);
    }

    private boolean sameAddress(Infrastructure left, Infrastructure right) {
        if (left.getAddress() == null || right.getAddress() == null) {
            return left.getAddress() == right.getAddress();
        }
        return left.getAddress().equals(right.getAddress());
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
