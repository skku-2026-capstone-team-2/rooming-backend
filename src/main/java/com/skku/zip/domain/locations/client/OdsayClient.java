package com.skku.zip.domain.locations.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.skku.zip.domain.locations.dto.OdsayRouteCandidate;
import com.skku.zip.domain.locations.entity.value.Minutes;
import com.skku.zip.domain.locations.entity.value.Path;
import com.skku.zip.domain.locations.entity.value.RoutePoint;
import com.skku.zip.domain.locations.entity.value.SubPath;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class OdsayClient {

    private static final String ROUTE_GRAPHIC_MAP_OBJECT_PREFIX = "0:0@";

    @Value("${odsay.api-key}")
    private String apiKey;

    @Value("${odsay.base-url}")
    private String baseUrl;

    private final RestClient restClient;

    public OdsayClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public Optional<OdsayRouteCandidate> findFastestRoute(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        Optional<JsonNode> responseOptional = searchPath(startLatitude, startLongitude, endLatitude, endLongitude);
        if (responseOptional.isEmpty()) {
            return Optional.empty();
        }

        JsonNode response = responseOptional.get();
        if (response == null || response.has("error")) {
            return Optional.empty();
        }

        JsonNode paths = response.path("result").path("path");
        if (!paths.isArray()) {
            return Optional.empty();
        }

        JsonNode selectedPathNode = null;
        int selectedTotalMinutes = Integer.MAX_VALUE;
        for (JsonNode pathNode : paths) {
            int totalMinutes = pathNode.path("info").path("totalTime").asInt(-1);
            if (totalMinutes >= 0 && totalMinutes < selectedTotalMinutes) {
                selectedPathNode = pathNode;
                selectedTotalMinutes = totalMinutes;
            }
        }

        if (selectedPathNode == null) {
            return Optional.empty();
        }

        JsonNode selectedInfo = selectedPathNode.path("info");
        Minutes selectedDuration = new Minutes(selectedTotalMinutes);
        Path routePath = new Path(
                selectedDuration,
                transferCount(selectedInfo),
                parseSubPaths(
                        selectedPathNode.path("subPath"),
                        loadRouteGraphics(text(selectedInfo, "mapObj"))
                )
        );
        return Optional.of(new OdsayRouteCandidate(selectedDuration, routePath));
    }

    private Optional<JsonNode> searchPath(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        try {
            Optional<JsonNode> response = Optional.ofNullable(restClient.get()
                    .uri(searchPathUri(startLatitude, startLongitude, endLatitude, endLongitude))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class));
            if (response.isEmpty()) {
                log.warn(
                        "ODSAY public transport route response was empty for start=({}, {}), end=({}, {})",
                        startLatitude,
                        startLongitude,
                        endLatitude,
                        endLongitude
                );
            }
            return response;
        } catch (RestClientException e) {
            log.warn(
                    "ODSAY public transport route request failed for start=({}, {}), end=({}, {}): {}",
                    startLatitude,
                    startLongitude,
                    endLatitude,
                    endLongitude,
                    e.getMessage()
            );
            return Optional.empty();
        }
    }

    private List<List<RoutePoint>> loadRouteGraphics(String mapObject) {
        if (mapObject == null) {
            return List.of();
        }

        try {
            JsonNode response = restClient.get()
                    .uri(routeGraphicUri(mapObject))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            return lanePoints(response == null ? null : response.path("result").path("lane"));
        } catch (RestClientException e) {
            log.warn("ODSAY route graphic request failed for mapObject={}: {}", mapObject, e.getMessage());
            return List.of();
        }
    }

    private List<List<RoutePoint>> lanePoints(JsonNode lanes) {
        if (lanes == null || !lanes.isArray()) {
            return List.of();
        }

        List<List<RoutePoint>> pointsByLane = new ArrayList<>();
        for (JsonNode lane : lanes) {
            pointsByLane.add(sectionPoints(lane.path("section")));
        }
        return pointsByLane;
    }

    private List<RoutePoint> sectionPoints(JsonNode sections) {
        if (!sections.isArray()) {
            return List.of();
        }

        List<RoutePoint> points = new ArrayList<>();
        for (JsonNode section : sections) {
            points.addAll(xyPoints(section.path("graphPos")));
        }
        return points;
    }

    private List<SubPath> parseSubPaths(JsonNode subPathNodes, List<List<RoutePoint>> routeGraphics) {
        if (!subPathNodes.isArray()) {
            return List.of();
        }

        List<SubPath> subPaths = new ArrayList<>();
        int routeGraphicIndex = 0;
        for (JsonNode subPathNode : subPathNodes) {
            int trafficType = subPathNode.path("trafficType").asInt();
            List<RoutePoint> points = routePoints(subPathNode);
            if (isPublicTransport(trafficType) && routeGraphicIndex < routeGraphics.size()) {
                List<RoutePoint> routeGraphic = routeGraphics.get(routeGraphicIndex);
                if (!routeGraphic.isEmpty()) {
                    points = routeGraphic;
                }
                routeGraphicIndex++;
            }

            subPaths.add(new SubPath(
                    trafficType,
                    new Minutes(subPathNode.path("sectionTime").asInt(0)),
                    text(subPathNode, "startName"),
                    text(subPathNode, "endName"),
                    laneName(subPathNode.path("lane")),
                    distanceMeters(subPathNode),
                    null,
                    points
            ));
        }
        return subPaths;
    }

    private boolean isPublicTransport(int trafficType) {
        return trafficType == 1 || trafficType == 2;
    }

    private int transferCount(JsonNode info) {
        if (info.has("transferCount")) {
            return info.path("transferCount").asInt(0);
        }

        int transitCount = info.path("busTransitCount").asInt(0)
                + info.path("subwayTransitCount").asInt(0);
        return Math.max(transitCount - 1, 0);
    }

    private Integer distanceMeters(JsonNode subPathNode) {
        Optional<Double> distance = number(subPathNode, "distance");
        if (distance.isEmpty() || distance.get() < 0) {
            return null;
        }
        return (int) Math.round(distance.get());
    }

    private List<RoutePoint> routePoints(JsonNode subPathNode) {
        List<RoutePoint> stopPoints = stationPoints(subPathNode.path("passStopList").path("stations"));
        if (!stopPoints.isEmpty()) {
            return stopPoints;
        }

        List<RoutePoint> endPoints = new ArrayList<>();
        routePoint(subPathNode, "startX", "startY").ifPresent(endPoints::add);
        routePoint(subPathNode, "endX", "endY").ifPresent(endPoints::add);
        return endPoints;
    }

    private List<RoutePoint> stationPoints(JsonNode stations) {
        return xyPoints(stations);
    }

    private List<RoutePoint> xyPoints(JsonNode nodes) {
        if (!nodes.isArray()) {
            return List.of();
        }

        List<RoutePoint> points = new ArrayList<>();
        for (JsonNode node : nodes) {
            routePoint(node, "x", "y").ifPresent(points::add);
        }
        return points;
    }

    private Optional<RoutePoint> routePoint(JsonNode node, String longitudeField, String latitudeField) {
        Optional<Double> longitude = number(node, longitudeField);
        Optional<Double> latitude = number(node, latitudeField);
        if (longitude.isEmpty() || latitude.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RoutePoint(latitude.get(), longitude.get()));
    }

    private Optional<Double> number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isNumber()) {
            return Optional.of(value.asDouble());
        }
        if (!value.isTextual() || value.asText().isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(value.asText()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private String laneName(JsonNode lanes) {
        if (lanes.isObject()) {
            return firstNonBlank(text(lanes, "busNo"), text(lanes, "name"));
        }
        if (!lanes.isArray() || lanes.isEmpty()) {
            return null;
        }

        JsonNode firstLane = lanes.get(0);
        return firstNonBlank(text(firstLane, "busNo"), text(firstLane, "name"));
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private String routeGraphicMapObject(String mapObject) {
        if (mapObject.startsWith(ROUTE_GRAPHIC_MAP_OBJECT_PREFIX)) {
            return mapObject;
        }
        return ROUTE_GRAPHIC_MAP_OBJECT_PREFIX + mapObject;
    }

    private URI searchPathUri(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        return URI.create(baseUrl + "/searchPubTransPathT"
                + "?SX=" + startLongitude
                + "&SY=" + startLatitude
                + "&EX=" + endLongitude
                + "&EY=" + endLatitude
                + "&apiKey=" + queryValue(apiKey));
    }

    private URI routeGraphicUri(String mapObject) {
        return URI.create(baseUrl + "/loadLane"
                + "?mapObject=" + queryValue(routeGraphicMapObject(mapObject))
                + "&apiKey=" + queryValue(apiKey));
    }

    private String queryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
