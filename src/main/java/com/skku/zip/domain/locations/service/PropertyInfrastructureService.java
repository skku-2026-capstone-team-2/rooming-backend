package com.skku.zip.domain.locations.service;

import com.skku.zip.domain.locations.client.OdsayClient;
import com.skku.zip.domain.locations.client.TmapClient;
import com.skku.zip.domain.locations.dto.TmapInfrastructureCandidate;
import com.skku.zip.domain.locations.entity.model.InfraAccessibility;
import com.skku.zip.domain.locations.entity.model.Infrastructure;
import com.skku.zip.domain.property.entity.Property;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PropertyInfrastructureService {

    private final TmapClient tmapClient;
    private final OdsayClient odsayClient;

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

    private Optional<InfraAccessibility> buildInfraAccessibility(Property property, Infrastructure infrastructure) {
        if (infrastructure.getId() == null) {
            throw new IllegalArgumentException("Save Infrastructure before creating InfraAccessibility.");
        }

        return odsayClient.findWalkingTime(
                        property.getLatitude(),
                        property.getLongitude(),
                        infrastructure.getLatitude(),
                        infrastructure.getLongitude()
                )
                .map(minutes -> new InfraAccessibility(property, infrastructure, minutes));
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
