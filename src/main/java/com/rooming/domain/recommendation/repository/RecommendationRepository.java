package com.rooming.domain.recommendation.repository;

import com.rooming.domain.recommendation.entity.Recommendation;
import com.rooming.domain.seeker.entity.Seeker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    Optional<Recommendation> findByIdAndSeeker(Long id, Seeker seeker);

    List<Recommendation> findAllBySeekerOrderByCreatedAtDescIdDesc(Seeker seeker);

    List<Recommendation> findAllBySeekerAndFavoriteTrueOrderByFavoritedAtDescCreatedAtDescIdDesc(Seeker seeker);
}