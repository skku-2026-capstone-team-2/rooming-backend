package com.skku.zip.domain.locations.repository;

import com.skku.zip.domain.locations.entity.model.Infrastructure;
import com.skku.zip.domain.locations.entity.type.INFRA_CATEGORY;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InfrastructureRepository extends JpaRepository<Infrastructure, Long> {
    List<Infrastructure> findAllByNameAndCategory(String name, INFRA_CATEGORY category);
}
