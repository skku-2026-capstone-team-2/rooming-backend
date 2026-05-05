package com.skku.zip.domain.favorite.repository;

import com.skku.zip.domain.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    boolean existsByPropertyId(Long propertyId);

    Optional<Favorite> findByPropertyId(Long propertyId);

    List<Favorite> findAllByOrderByCreatedAtDesc();
}
