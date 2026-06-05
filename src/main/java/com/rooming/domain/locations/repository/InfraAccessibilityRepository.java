package com.rooming.domain.locations.repository;

import com.rooming.domain.locations.entity.model.InfraAccessibility;
import com.rooming.domain.locations.entity.id.PropertyInfrastructureId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InfraAccessibilityRepository extends JpaRepository<InfraAccessibility, PropertyInfrastructureId> {
    boolean existsByPropertyPropertyId(Long propertyId);

    List<InfraAccessibility> findAllByPropertyPropertyId(Long propertyId);

    long countByIdInfrastructureId(Long infrastructureId);
}
