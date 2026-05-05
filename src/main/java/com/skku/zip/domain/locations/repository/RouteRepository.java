package com.skku.zip.domain.locations.repository;

import com.skku.zip.domain.locations.entity.model.Route;
import com.skku.zip.domain.locations.entity.id.UserplacePropertyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, UserplacePropertyId> {
}
