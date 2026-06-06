package com.rooming.domain.locations.repository;

import com.rooming.domain.locations.entity.model.InfraAccessibility;
import com.rooming.domain.locations.entity.id.PropertyInfrastructureId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InfraAccessibilityRepository extends JpaRepository<InfraAccessibility, PropertyInfrastructureId> {
    boolean existsByPropertyPropertyId(Long propertyId);

    List<InfraAccessibility> findAllByPropertyPropertyId(Long propertyId);

    long countByIdInfrastructureId(Long infrastructureId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM infra_accessibilities ia
                WHERE ia.property_id = :propertyId
                  AND ia.infrastructure_id = :infrastructureId
                  AND ia.walking_time IS NOT NULL
                  AND ia.walking_route_json IS NOT NULL
            )
            """, nativeQuery = true)
    boolean existsValidAccessibility(
            @Param("propertyId") Long propertyId,
            @Param("infrastructureId") Long infrastructureId
    );

    @Modifying
    @Query(value = """
            DELETE FROM infra_accessibilities
            WHERE walking_time IS NULL
               OR walking_route_json IS NULL
            """, nativeQuery = true)
    int deleteInvalidAccessibilities();
}
