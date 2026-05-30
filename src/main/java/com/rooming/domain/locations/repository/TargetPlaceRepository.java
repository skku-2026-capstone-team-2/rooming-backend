package com.rooming.domain.locations.repository;

import com.rooming.domain.locations.entity.model.TargetPlace;
import com.rooming.domain.locations.entity.value.RoadAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TargetPlaceRepository extends JpaRepository<TargetPlace, Long> {
    Optional<TargetPlace> findByAddress(RoadAddress address);
}