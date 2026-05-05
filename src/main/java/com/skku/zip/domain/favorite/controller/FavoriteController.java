package com.skku.zip.domain.favorite.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.skku.zip.common.dto.ApiResponse;
import com.skku.zip.domain.favorite.dto.FavoriteListData;
import com.skku.zip.domain.favorite.dto.FavoriteResponseItem;
import com.skku.zip.domain.favorite.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ApiResponse<FavoriteListData> getFavorites() {
        return ApiResponse.success(
                new FavoriteListData(favoriteService.getFavorites()),
                "Favorite list fetched."
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FavoriteResponseItem> createFavorite(@RequestBody JsonNode snapshot) {
        return ApiResponse.success(
                favoriteService.createFavorite(snapshot),
                "Favorite saved."
        );
    }

    @GetMapping("/{favoriteId}")
    public ApiResponse<FavoriteResponseItem> getFavorite(@PathVariable Long favoriteId) {
        return ApiResponse.success(
                favoriteService.getFavorite(favoriteId),
                "Favorite fetched."
        );
    }

    @DeleteMapping("/{favoriteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFavorite(@PathVariable Long favoriteId) {
        favoriteService.deleteFavorite(favoriteId);
    }
}
