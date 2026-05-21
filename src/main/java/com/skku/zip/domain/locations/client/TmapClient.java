package com.skku.zip.domain.locations.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.skku.zip.domain.locations.dto.OdsayRouteCandidate;
import com.skku.zip.domain.locations.dto.TmapInfrastructureCandidate;
import com.skku.zip.domain.locations.dto.TmapPlaceCandidate;
import com.skku.zip.domain.locations.entity.type.INFRA_CATEGORY;
import com.skku.zip.domain.locations.entity.value.Minutes;
import com.skku.zip.domain.locations.entity.value.Path;
import com.skku.zip.domain.locations.entity.value.RoadAddress;
import com.skku.zip.domain.locations.entity.value.RoutePoint;
import com.skku.zip.domain.locations.entity.value.SubPath;
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
public class TmapClient {

    private static final int MAX_POI_COUNT = 200;
    private static final int WALK_TRAFFIC_TYPE = 3;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final double WALKING_METERS_PER_MINUTE = 67.0;
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

    private final RestClient restClient;

    public TmapClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
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

    public Optional<OdsayRouteCandidate> findWalkingRoute(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("startX", startLongitude);
        request.put("startY", startLatitude);
        request.put("endX", endLongitude);
        request.put("endY", endLatitude);
        request.put("reqCoordType", "WGS84GEO");
        request.put("resCoordType", "WGS84GEO");
        request.put("startName", "start");
        request.put("endName", "end");
        request.put("searchOption", "0");
        request.put("sort", "index");

        try {
            JsonNode response = restClient.post()
                    .uri(baseUrl + "/routes/pedestrian?version=1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("appKey", apiKey)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            return toWalkingRoute(response, startLatitude, startLongitude, endLatitude, endLongitude);
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    private Optional<OdsayRouteCandidate> toWalkingRoute(
            JsonNode response,
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        if (response == null || response.has("error")) {
            return Optional.empty();
        }

        JsonNode features = response.path("features");
        if (!features.isArray()) {
            return Optional.empty();
        }

        int totalSeconds = -1;
        int totalDistanceMeters = -1;
        int summedSectionSeconds = 0;
        List<SubPath> subPaths = new ArrayList<>();

        for (JsonNode feature : features) {
            JsonNode properties = feature.path("properties");
            if (totalSeconds < 0) {
                totalSeconds = properties.path("totalTime").asInt(-1);
            }
            if (totalDistanceMeters < 0) {
                totalDistanceMeters = properties.path("totalDistance").asInt(-1);
            }

            JsonNode geometry = feature.path("geometry");
            if (!"LineString".equals(geometry.path("type").asText())) {
                continue;
            }

            int sectionSeconds = properties.path("time").asInt(0);
            summedSectionSeconds += sectionSeconds;

            int distanceMeters = properties.path("distance").asInt(-1);
            String roadName = firstNonBlank(text(properties, "name"), text(properties, "roadName"));
            String description = firstNonBlank(text(properties, "description"), roadName);

            subPaths.add(new SubPath(
                    WALK_TRAFFIC_TYPE,
                    secondsToMinutes(sectionSeconds),
                    null,
                    null,
                    roadName,
                    distanceMeters >= 0 ? distanceMeters : null,
                    description,
                    routePoints(geometry.path("coordinates"))
            ));
        }

        if (totalSeconds < 0 && summedSectionSeconds > 0) {
            totalSeconds = summedSectionSeconds;
        }

        Optional<Minutes> duration = walkingDuration(totalSeconds, totalDistanceMeters);
        if (duration.isEmpty()) {
            return Optional.empty();
        }

        if (subPaths.isEmpty()) {
            subPaths = List.of(new SubPath(
                    WALK_TRAFFIC_TYPE,
                    duration.get(),
                    null,
                    null,
                    null,
                    totalDistanceMeters >= 0 ? totalDistanceMeters : null,
                    null,
                    List.of(
                            new RoutePoint(startLatitude, startLongitude),
                            new RoutePoint(endLatitude, endLongitude)
                    )
            ));
        }

        return Optional.of(new OdsayRouteCandidate(
                duration.get(),
                new Path(duration.get(), 0, subPaths)
        ));
    }

    private Optional<Minutes> walkingDuration(int totalSeconds, int totalDistanceMeters) {
        if (totalSeconds >= 0) {
            return Optional.of(secondsToMinutes(totalSeconds));
        }
        if (totalDistanceMeters > 0) {
            return Optional.of(estimateWalkingMinutes(totalDistanceMeters));
        }
        return Optional.empty();
    }

    private Minutes secondsToMinutes(int seconds) {
        return new Minutes((int) Math.ceil((double) seconds / SECONDS_PER_MINUTE));
    }

    private Minutes estimateWalkingMinutes(int distanceMeters) {
        return new Minutes((int) Math.ceil(distanceMeters / WALKING_METERS_PER_MINUTE));
    }

    private List<RoutePoint> routePoints(JsonNode coordinates) {
        if (!coordinates.isArray()) {
            return List.of();
        }

        List<RoutePoint> points = new ArrayList<>();
        for (JsonNode coordinate : coordinates) {
            if (!coordinate.isArray() || coordinate.size() < 2) {
                continue;
            }

            JsonNode longitude = coordinate.get(0);
            JsonNode latitude = coordinate.get(1);
            if (longitude.isNumber() && latitude.isNumber()) {
                points.add(new RoutePoint(latitude.asDouble(), longitude.asDouble()));
                continue;
            }
            points.addAll(routePoints(coordinate));
        }
        return points;
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

    private Optional<TmapInfrastructureCandidate> toInfrastructureCandidate(INFRA_CATEGORY fallbackCategory, JsonNode poi) {
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
        INFRA_CATEGORY category = INFRA_CATEGORY.fromMiddleBizName(text(poi, "middleBizName"))
                .orElse(fallbackCategory);

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

}
