package com.skku.zip.domain.favorite.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.skku.zip.common.exception.ConflictException;
import com.skku.zip.common.exception.NotFoundException;
import com.skku.zip.domain.favorite.dto.FavoriteResponseItem;
import com.skku.zip.domain.favorite.dto.FavoriteSummary;
import com.skku.zip.domain.favorite.entity.Favorite;
import com.skku.zip.domain.favorite.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    @Transactional(readOnly = true)
    public List<FavoriteSummary> getFavorites() {
        return favoriteRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public FavoriteResponseItem createFavorite(JsonNode snapshot) {
        Favorite favorite = new Favorite(snapshot);
        if (favoriteRepository.existsByPropertyId(favorite.getPropertyId())) {
            throw new ConflictException("Favorite already exists for this property.");
        }

        return toResponse(favoriteRepository.save(favorite));
    }

    @Transactional(readOnly = true)
    public FavoriteResponseItem getFavorite(Long favoriteId) {
        return toResponse(findFavorite(favoriteId));
    }

    @Transactional
    public void deleteFavorite(Long favoriteId) {
        Favorite favorite = findFavorite(favoriteId);
        favoriteRepository.delete(favorite);
    }

    private Favorite findFavorite(Long favoriteId) {
        return favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new NotFoundException("Favorite not found."));
    }

    private FavoriteSummary toSummary(Favorite favorite) {
        return new FavoriteSummary(
                favorite.getId(),
                favorite.getPropertyId(),
                favorite.getTitle(),
                favorite.getRoadAddress(),
                favorite.getCreatedAt()
        );
    }

    private FavoriteResponseItem toResponse(Favorite favorite) {
        return new FavoriteResponseItem(
                favorite.getId(),
                favorite.getPropertyId(),
                favorite.getSnapshotJson(),
                favorite.getCreatedAt(),
                favorite.getUpdatedAt()
        );
    }
}
