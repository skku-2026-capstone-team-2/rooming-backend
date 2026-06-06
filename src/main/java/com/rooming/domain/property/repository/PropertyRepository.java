package com.rooming.domain.property.repository;

import com.rooming.domain.property.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    @Query(value = """
            SELECT p.*
            FROM properties p
            WHERE p.latitude IS NOT NULL
              AND p.longitude IS NOT NULL
            ORDER BY
                CASE
                    WHEN COALESCE(p.nearby_infrastructures_fetched, false) = false THEN 0
                    WHEN COALESCE(p.infra_accessibilities_fetched, false) = false THEN 1
                    ELSE 2
                END,
                p.property_id
            """, nativeQuery = true)
    List<Property> findPropertiesForDailyInfrastructureSync();

    List<Property> findAllByLatitudeIsNotNullAndLongitudeIsNotNull();
}
