package com.skku.zip.domain.locations.dto;

import java.util.List;

public record UserPlaceListData(
        List<UserPlaceResponseItem> keyPlaces
) {
}
