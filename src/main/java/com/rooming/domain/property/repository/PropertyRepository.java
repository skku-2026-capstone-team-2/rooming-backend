package com.rooming.domain.property.repository;

import com.rooming.domain.property.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    @Query(value = """
            SELECT p.*
            FROM properties p
            LEFT JOIN property_infrastructure_sync_state s
                ON s.property_id = p.property_id
            WHERE p.latitude IS NOT NULL
              AND p.longitude IS NOT NULL
            ORDER BY
                CASE WHEN EXISTS (
                    SELECT 1
                    FROM infra_accessibilities ia
                    WHERE ia.property_id = p.property_id
                ) THEN 1 ELSE 0 END,
                CASE WHEN s.last_refreshed_at IS NULL THEN 0 ELSE 1 END,
                s.last_refreshed_at ASC,
                p.property_id
            """, nativeQuery = true)
    List<Property> findPropertiesForDailyInfrastructureSync();
}
