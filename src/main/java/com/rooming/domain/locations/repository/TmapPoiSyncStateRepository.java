package com.rooming.domain.locations.repository;

import com.rooming.domain.locations.entity.model.TmapPoiSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmapPoiSyncStateRepository extends JpaRepository<TmapPoiSyncState, String> {
}
