package com.skku.zip.domain.locations.repository;

import com.skku.zip.domain.locations.entity.InfraAccessibility;
import com.skku.zip.domain.locations.entity.PropertyInfraId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InfraAccessibilityRepository extends JpaRepository<InfraAccessibility, PropertyInfraId> {
}
