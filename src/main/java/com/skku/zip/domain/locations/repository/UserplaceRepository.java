package com.skku.zip.domain.locations.repository;

import com.skku.zip.domain.locations.entity.Userplace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserplaceRepository extends JpaRepository<Userplace, Long> {
    List<Userplace> findAllByOrderByIdAsc();
}
