package com.skku.zip.domain.locations.controller;

import com.skku.zip.common.dto.ApiResponse;
import com.skku.zip.domain.locations.dto.PlaceSearchData;
import com.skku.zip.domain.locations.service.TargetPlaceApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/places")
public class PlaceSearchController {

    private final TargetPlaceApiService targetPlaceApiService;

    @GetMapping("/search")
    public ApiResponse<PlaceSearchData> searchPlaces(@RequestParam String keyword) {
        return ApiResponse.success(
                new PlaceSearchData(targetPlaceApiService.searchPlaces(keyword)),
                "Place search completed."
        );
    }
}
