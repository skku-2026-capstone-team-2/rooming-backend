package com.skku.zip.domain.favorite.dto;

import java.util.List;

public record FavoriteListData(
        List<FavoriteSummary> favorites
) {
}
