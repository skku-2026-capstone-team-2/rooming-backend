package com.skku.zip.domain.seeker.repository;

import com.skku.zip.domain.seeker.entity.Seeker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeekerRepository extends JpaRepository<Seeker, Long> {
    Optional<Seeker> findByLoginId(String loginId);
}
