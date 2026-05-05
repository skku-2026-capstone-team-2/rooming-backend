package com.skku.zip.domain.favorite.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Timestamp;

public record FavoriteResponseItem(
        Long favoriteId,
        Long propertyId,
        JsonNode snapshot,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
