package com.skku.zip.domain.user.repository;

import com.skku.zip.domain.user.entity.Seeker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeekerRepository extends JpaRepository<Seeker, Long> {
    Optional<Seeker> findByLoginId(String loginId);
}
