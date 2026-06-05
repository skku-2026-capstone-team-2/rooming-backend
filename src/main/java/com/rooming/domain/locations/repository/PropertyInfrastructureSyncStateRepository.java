package com.rooming.domain.locations.repository;

import com.rooming.domain.locations.entity.model.PropertyInfrastructureSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyInfrastructureSyncStateRepository
        extends JpaRepository<PropertyInfrastructureSyncState, Long> {
}
