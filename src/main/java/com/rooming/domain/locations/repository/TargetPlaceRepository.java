package com.rooming.domain.locations.repository;

import com.rooming.domain.locations.entity.model.TargetPlace;
import com.rooming.domain.locations.entity.value.RoadAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TargetPlaceRepository extends JpaRepository<TargetPlace, Long> {
    Optional<TargetPlace> findByAddress(RoadAddress address);

    @Query(value = """
            SELECT tp.*
            FROM target_places tp
            WHERE ST_DWithin(
                tp.location,
                CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                :radiusMeters
            )
            ORDER BY ST_Distance(
                tp.location,
                CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)
            )
            """, nativeQuery = true)
    List<TargetPlace> findWithinMeters(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters
    );
}
