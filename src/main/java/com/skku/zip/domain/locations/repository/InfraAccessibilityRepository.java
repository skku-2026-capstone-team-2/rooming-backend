package com.skku.zip.domain.locations.repository;

import com.skku.zip.domain.locations.entity.model.InfraAccessibility;
import com.skku.zip.domain.locations.entity.id.PropertyInfrastructureId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InfraAccessibilityRepository extends JpaRepository<InfraAccessibility, PropertyInfrastructureId> {
}
