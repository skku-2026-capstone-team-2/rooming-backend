package com.skku.zip.domain.locations.repository;

import com.skku.zip.domain.locations.entity.model.TargetPlace;
import com.skku.zip.domain.locations.entity.value.RoadAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TargetPlaceRepository extends JpaRepository<TargetPlace, Long> {
    Optional<TargetPlace> findByAddress(RoadAddress address);
}
