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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class OdsayClient {

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
                selectedInfo.path("transferCount").asInt(0),
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
            return Optional.ofNullable(restClient.get()
                    .uri(baseUrl + "/searchPubTransPathT"
                                    + "?SX={startLongitude}&SY={startLatitude}"
                                    + "&EX={endLongitude}&EY={endLatitude}"
                                    + "&OPT=0&SearchType=0&apiKey={apiKey}",
                            startLongitude,
                            startLatitude,
                            endLongitude,
                            endLatitude,
                            apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class));
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    private List<List<RoutePoint>> loadRouteGraphics(String mapObject) {
        if (mapObject == null) {
            return List.of();
        }

        try {
            JsonNode response = restClient.get()
                    .uri(baseUrl + "/loadLane?mapObject={mapObject}&apiKey={apiKey}", mapObject, apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            return lanePoints(response == null ? null : response.path("result").path("lane"));
        } catch (RestClientException e) {
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
        if (!lanes.isArray() || lanes.isEmpty()) {
            return null;
        }

        JsonNode firstLane = lanes.get(0);
        String busNo = text(firstLane, "busNo");
        if (busNo != null) {
            return busNo;
        }
        return text(firstLane, "name");
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }
}
