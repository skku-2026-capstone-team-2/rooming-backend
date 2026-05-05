package com.skku.zip.domain.locations.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.skku.zip.domain.locations.dto.TmapInfrastructureCandidate;
import com.skku.zip.domain.locations.dto.TmapPlaceCandidate;
import com.skku.zip.domain.locations.dto.TmapUserplaceInfo;
import com.skku.zip.domain.locations.entity.type.INFRA_CATEGORY;
import com.skku.zip.domain.locations.entity.type.PLACE_CATEGORY;
import com.skku.zip.domain.locations.entity.value.RoadAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TmapClient {

    private static final String DEFAULT_NAME = "Selected place";
    private static final int MAX_POI_COUNT = 200;
    private static final String POI_CATEGORIES =
            "\uD559\uAD50;\uC9C0\uD558\uCCA0;\uBC84\uC2A4\uC815\uB958\uC7A5;\uBC84\uC2A4;\uC8FC\uC694\uC2DC\uC124\uBB3C;\uAD00\uACF5\uC11C";
    private static final Map<INFRA_CATEGORY, String> INFRA_CATEGORY_QUERIES = Map.ofEntries(
            Map.entry(INFRA_CATEGORY.CONVENIENT_STORE, "\uD3B8\uC758\uC810"),
            Map.entry(INFRA_CATEGORY.MART, "\uB9C8\uD2B8;\uB300\uD615\uB9C8\uD2B8"),
            Map.entry(INFRA_CATEGORY.PHARMACY, "\uC57D\uAD6D"),
            Map.entry(INFRA_CATEGORY.HOSPITAL, "\uBCD1\uC6D0"),
            Map.entry(INFRA_CATEGORY.LAUNDRY, "\uC138\uD0C1\uC18C"),
            Map.entry(INFRA_CATEGORY.CAFE, "\uCE74\uD398;\uCEE4\uD53C"),
            Map.entry(INFRA_CATEGORY.SUBWAY, "\uC9C0\uD558\uCCA0"),
            Map.entry(INFRA_CATEGORY.BANK, "\uC740\uD589;ATM"),
            Map.entry(INFRA_CATEGORY.GYM, "\uD5EC\uC2A4\uD074\uB7FD;\uB808\uC800"),
            Map.entry(INFRA_CATEGORY.KARAOKE, "\uB178\uB798\uBC29"),
            Map.entry(INFRA_CATEGORY.PC_ROOM, "PC\uBC29")
    );

    @Value("${tmap.api-key}")
    private String apiKey;

    @Value("${tmap.base-url}")
    private String baseUrl;

    private final RestClient restClient = RestClient.create();

    public TmapUserplaceInfo resolveUserplace(double latitude, double longitude) {
        String address = findAddress(latitude, longitude);
        TmapPoi poi = findNearestPoi(latitude, longitude);

        String name = firstNonBlank(poi.name(), address, DEFAULT_NAME);
        PLACE_CATEGORY category = mapCategory(poi.category());
        RoadAddress roadAddress = address == null || address.isBlank() ? null : new RoadAddress(address);

        return new TmapUserplaceInfo(name, category, roadAddress);
    }

    public List<TmapInfrastructureCandidate> findInfrastructureCandidates(
            double latitude,
            double longitude,
            int radiusKm
    ) {
        Map<String, TmapInfrastructureCandidate> candidates = new LinkedHashMap<>();

        for (INFRA_CATEGORY category : INFRA_CATEGORY.values()) {
            JsonNode pois = findPoisAround(latitude, longitude, radiusKm, INFRA_CATEGORY_QUERIES.get(category));
            if (pois == null || !pois.isArray()) {
                continue;
            }

            for (JsonNode poi : pois) {
                toInfrastructureCandidate(category, poi)
                        .ifPresent(candidate -> candidates.putIfAbsent(dedupKey(candidate), candidate));
            }
        }

        return new ArrayList<>(candidates.values());
    }

    public List<TmapPlaceCandidate> searchPlaces(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        try {
            JsonNode response = restClient.get()
                    .uri(baseUrl + "/pois"
                                    + "?version=1&searchKeyword={keyword}&searchType=all"
                                    + "&page=1&count=10&resCoordType=WGS84GEO&multiPoint=N"
                                    + "&searchtypCd=A&reqCoordType=WGS84GEO&poiGroupYn=N",
                            keyword.trim())
                    .accept(MediaType.APPLICATION_JSON)
                    .header("appKey", apiKey)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode pois = response == null
                    ? null
                    : response.path("searchPoiInfo").path("pois").path("poi");

            if (pois == null || !pois.isArray()) {
                return List.of();
            }

            List<TmapPlaceCandidate> places = new ArrayList<>();
            for (JsonNode poi : pois) {
                toPlaceCandidate(poi).ifPresent(places::add);
            }
            return places;
        } catch (RestClientException e) {
            return List.of();
        }
    }

    private String findAddress(double latitude, double longitude) {
        JsonNode response = restClient.get()
                .uri(baseUrl + "/geo/reversegeocoding"
                                + "?version=1&lat={latitude}&lon={longitude}"
                                + "&coordType=WGS84GEO&addressType=A10&newAddressExtend=Y",
                        latitude,
                        longitude)
                .accept(MediaType.APPLICATION_JSON)
                .header("appKey", apiKey)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            return null;
        }

        JsonNode addressInfo = response.path("addressInfo");
        return firstNonBlank(
                text(addressInfo, "fullAddress"),
                text(addressInfo, "roadAddress"),
                text(addressInfo, "legalDong"),
                text(addressInfo, "adminDong")
        );
    }

    private TmapPoi findNearestPoi(double latitude, double longitude) {
        try {
            JsonNode response = restClient.get()
                    .uri(baseUrl + "/pois/search/around"
                                    + "?version=1&centerLon={longitude}&centerLat={latitude}"
                                    + "&radius=1&count=1&page=1&categories={categories}"
                                    + "&resCoordType=WGS84GEO&reqCoordType=WGS84GEO",
                            longitude,
                            latitude,
                            POI_CATEGORIES)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("appKey", apiKey)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode firstPoi = response == null
                    ? null
                    : response.path("searchPoiInfo").path("pois").path("poi").path(0);

            if (firstPoi == null || firstPoi.isMissingNode()) {
                return TmapPoi.empty();
            }

            return new TmapPoi(
                    firstNonBlank(text(firstPoi, "name"), text(firstPoi, "newAddressList.newAddress.fullAddressRoad")),
                    firstNonBlank(text(firstPoi, "bizCatName"), text(firstPoi, "upperBizName"), text(firstPoi, "middleBizName"))
            );
        } catch (RestClientException e) {
            return TmapPoi.empty();
        }
    }

    private JsonNode findPoisAround(double latitude, double longitude, int radiusKm, String categories) {
        try {
            JsonNode response = restClient.get()
                    .uri(baseUrl + "/pois/search/around"
                                    + "?version=1&centerLon={longitude}&centerLat={latitude}"
                                    + "&radius={radiusKm}&count={count}&page=1&categories={categories}"
                                    + "&resCoordType=WGS84GEO&reqCoordType=WGS84GEO",
                            longitude,
                            latitude,
                            radiusKm,
                            MAX_POI_COUNT,
                            categories)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("appKey", apiKey)
                    .retrieve()
                    .body(JsonNode.class);

            return response == null
                    ? null
                    : response.path("searchPoiInfo").path("pois").path("poi");
        } catch (RestClientException e) {
            return null;
        }
    }

    private Optional<TmapInfrastructureCandidate> toInfrastructureCandidate(INFRA_CATEGORY category, JsonNode poi) {
        String name = text(poi, "name");
        Optional<Double> latitude = firstDouble(poi, "frontLat", "noorLat", "centerLat");
        Optional<Double> longitude = firstDouble(poi, "frontLon", "noorLon", "centerLon");

        if (name == null || latitude.isEmpty() || longitude.isEmpty()) {
            return Optional.empty();
        }

        String address = firstNonBlank(
                text(poi, "newAddressList.newAddress.fullAddressRoad"),
                roadAddressText(poi),
                jibunAddressText(poi)
        );

        return Optional.of(new TmapInfrastructureCandidate(
                firstNonBlank(text(poi, "id"), text(poi, "pkey")),
                name,
                category,
                latitude.get(),
                longitude.get(),
                address == null ? null : new RoadAddress(address)
        ));
    }

    private Optional<TmapPlaceCandidate> toPlaceCandidate(JsonNode poi) {
        String name = text(poi, "name");
        Optional<Double> latitude = firstDouble(poi, "frontLat", "noorLat", "centerLat");
        Optional<Double> longitude = firstDouble(poi, "frontLon", "noorLon", "centerLon");

        if (name == null || latitude.isEmpty() || longitude.isEmpty()) {
            return Optional.empty();
        }

        String address = firstNonBlank(
                text(poi, "newAddressList.newAddress.fullAddressRoad"),
                roadAddressText(poi),
                jibunAddressText(poi)
        );

        return Optional.of(new TmapPlaceCandidate(
                firstNonBlank(text(poi, "id"), text(poi, "pkey")),
                name,
                address,
                latitude.get(),
                longitude.get()
        ));
    }

    private PLACE_CATEGORY mapCategory(String category) {
        if (category == null) {
            return PLACE_CATEGORY.COMPANY;
        }

        String normalized = category.toLowerCase();
        if (normalized.contains("\uD559\uAD50") || normalized.contains("\uB300\uD559") || normalized.contains("school")) {
            return PLACE_CATEGORY.SCHOOL;
        }
        if (normalized.contains("\uC9C0\uD558\uCCA0") || normalized.contains("subway")) {
            return PLACE_CATEGORY.SUBWAY_STATION;
        }
        if (normalized.contains("\uBC84\uC2A4") || normalized.contains("\uD130\uBBF8\uB110") || normalized.contains("bus")) {
            return PLACE_CATEGORY.BUS_TERMINAL;
        }
        return PLACE_CATEGORY.COMPANY;
    }

    private String text(JsonNode node, String path) {
        JsonNode current = node;
        for (String segment : path.split("\\.")) {
            current = current.path(segment);
        }
        return current.isMissingNode() || current.isNull() ? null : current.asText();
    }

    private Optional<Double> firstDouble(JsonNode node, String... paths) {
        for (String path : paths) {
            String value = text(node, path);
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                return Optional.of(Double.parseDouble(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return Optional.empty();
    }

    private String roadAddressText(JsonNode poi) {
        String roadName = text(poi, "roadName");
        if (roadName == null) {
            return null;
        }

        String buildingNo1 = text(poi, "buildingNo1");
        String buildingNo2 = text(poi, "buildingNo2");
        if (buildingNo1 == null || buildingNo1.isBlank() || "0".equals(buildingNo1)) {
            return roadName;
        }
        String buildingNumber = "0".equals(buildingNo2) || buildingNo2 == null
                ? buildingNo1
                : buildingNo1 + "-" + buildingNo2;

        return firstNonBlank(roadName + " " + buildingNumber, roadName);
    }

    private String jibunAddressText(JsonNode poi) {
        return firstNonBlank(
                joinAddressParts(
                        text(poi, "upperAddrName"),
                        text(poi, "middleAddrName"),
                        text(poi, "lowerAddrName"),
                        text(poi, "detailAddrName"),
                        text(poi, "firstNo")
                ),
                text(poi, "address")
        );
    }

    private String joinAddressParts(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank() || "0".equals(part)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.trim());
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String dedupKey(TmapInfrastructureCandidate candidate) {
        if (candidate.externalId() != null && !candidate.externalId().isBlank()) {
            return candidate.category() + ":" + candidate.externalId();
        }
        return candidate.category() + ":"
                + candidate.name() + ":"
                + Math.round(candidate.latitude() * 1_000_000) + ":"
                + Math.round(candidate.longitude() * 1_000_000);
    }

    private record TmapPoi(String name, String category) {
        private static TmapPoi empty() {
            return new TmapPoi(null, null);
        }
    }
}
