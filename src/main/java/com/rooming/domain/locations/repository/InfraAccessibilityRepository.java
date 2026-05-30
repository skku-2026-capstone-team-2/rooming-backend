package com.rooming.domain.locations.repository;

import com.rooming.domain.locations.entity.model.InfraAccessibility;
import com.rooming.domain.locations.entity.id.PropertyInfrastructureId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InfraAccessibilityRepository extends JpaRepository<InfraAccessibility, PropertyInfrastructureId> {
}