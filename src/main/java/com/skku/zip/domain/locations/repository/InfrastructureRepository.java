package com.skku.zip.domain.locations.repository;

import com.skku.zip.domain.locations.entity.model.Infrastructure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InfrastructureRepository extends JpaRepository<Infrastructure, Long> {
}
