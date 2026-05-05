package com.skku.zip.domain.locations.repository;

import com.skku.zip.domain.locations.entity.Route;
import com.skku.zip.domain.locations.entity.UserplacePropertyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, UserplacePropertyId> {
}
