package com.rooming.domain.locations.repository;

import com.rooming.domain.locations.entity.id.TargetPlacePropertyId;
import com.rooming.domain.locations.entity.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, TargetPlacePropertyId> {
}