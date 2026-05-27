package com.skku.zip.domain.locations.repository;

import com.skku.zip.domain.locations.entity.model.Infrastructure;
import com.skku.zip.domain.locations.entity.type.INFRA_CATEGORY;
import com.skku.zip.domain.locations.entity.value.RoadAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InfrastructureRepository extends JpaRepository<Infrastructure, Long> {
    List<Infrastructure> findAllByNameAndCategory(String name, INFRA_CATEGORY category);
    Optional<Infrastructure> findByAddress(RoadAddress address);

    @Query(value = """
            SELECT *
            FROM infrastructures i
            WHERE i.location IS NOT NULL
              AND ST_DWithin(
                    i.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :radiusMeters
                  )
            ORDER BY ST_Distance(
                    i.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                  )
            LIMIT :limit
            """, nativeQuery = true)
    List<Infrastructure> findWithinMeters(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("limit") int limit
    );
}
