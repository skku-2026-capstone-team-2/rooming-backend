package com.skku.zip.domain.locations.controller;

import com.skku.zip.common.dto.ApiResponse;
import com.skku.zip.domain.locations.dto.UserPlaceCreateRequest;
import com.skku.zip.domain.locations.dto.UserPlaceListData;
import com.skku.zip.domain.locations.dto.UserPlaceResponseItem;
import com.skku.zip.domain.locations.dto.UserPlaceUpdateRequest;
import com.skku.zip.domain.locations.service.UserPlaceApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/user-places")
public class UserPlaceController {

    private final UserPlaceApiService userPlaceApiService;

    @GetMapping
    public ApiResponse<UserPlaceListData> getUserPlaces() {
        return ApiResponse.success(
                new UserPlaceListData(userPlaceApiService.getUserPlaces()),
                "User place list fetched."
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserPlaceResponseItem> createUserPlace(@Valid @RequestBody UserPlaceCreateRequest request) {
        return ApiResponse.success(
                userPlaceApiService.createUserPlace(request),
                "User place saved."
        );
    }

    @PutMapping("/{userPlaceId}")
    public ApiResponse<UserPlaceResponseItem> updateUserPlace(
            @PathVariable Long userPlaceId,
            @Valid @RequestBody UserPlaceUpdateRequest request
    ) {
        return ApiResponse.success(
                userPlaceApiService.updateUserPlace(userPlaceId, request),
                "User place updated."
        );
    }

    @DeleteMapping("/{userPlaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserPlace(@PathVariable Long userPlaceId) {
        userPlaceApiService.deleteUserPlace(userPlaceId);
    }
}
