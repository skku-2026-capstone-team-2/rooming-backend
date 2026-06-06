package com.rooming.domain.locations.repository;

import com.rooming.domain.locations.entity.model.Infrastructure;
import com.rooming.domain.locations.entity.value.RoadAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InfrastructureRepository extends JpaRepository<Infrastructure, Long> {
    Optional<Infrastructure> findByAddress(RoadAddress address);

    @Query(value = """
            SELECT *
            FROM infrastructures i
            WHERE i.location IS NOT NULL
              AND ST_DWithin(
                    i.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :toleranceMeters
                  )
            ORDER BY ST_Distance(
                    i.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                  )
            LIMIT 1
            """, nativeQuery = true)
    Optional<Infrastructure> findFirstByLocationWithinMeters(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("toleranceMeters") double toleranceMeters
    );

    @Query(value = """
            SELECT *
            FROM infrastructures i
            WHERE i.location IS NOT NULL
              AND i.category IS NOT NULL
              AND i.category <> 'ETC'
              AND ST_DWithin(
                    i.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :radiusMeters
                  )
            ORDER BY ST_Distance(
                    i.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                  )
            """, nativeQuery = true)
    List<Infrastructure> findNearbyNonEtcWithinMeters(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters
    );
}
