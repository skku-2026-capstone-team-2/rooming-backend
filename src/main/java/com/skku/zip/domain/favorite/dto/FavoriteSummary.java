package com.skku.zip.domain.favorite.dto;

import java.sql.Timestamp;

public record FavoriteSummary(
        Long favoriteId,
        Long propertyId,
        String title,
        String roadAddress,
        Timestamp createdAt
) {
}
