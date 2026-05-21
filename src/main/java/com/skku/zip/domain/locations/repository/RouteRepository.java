package com.skku.zip.domain.locations.repository;

import com.skku.zip.domain.locations.entity.id.TargetPlacePropertyId;
import com.skku.zip.domain.locations.entity.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, TargetPlacePropertyId> {
}
