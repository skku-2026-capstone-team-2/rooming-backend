package com.skku.zip.domain.locations.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.skku.zip.domain.locations.dto.OdsayRouteCandidate;
import com.skku.zip.domain.locations.entity.value.Minutes;
import com.skku.zip.domain.locations.entity.value.Path;
import com.skku.zip.domain.locations.entity.value.SubPath;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class OdsayClient {

    private static final double WALKING_METERS_PER_MINUTE = 67.0;

    @Value("${odsay.api-key}")
    private String apiKey;

    @Value("${odsay.base-url}")
    private String baseUrl;

    private final RestClient restClient = RestClient.create();

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

        List<OdsayRouteCandidate> candidates = new ArrayList<>();
        for (JsonNode pathNode : paths) {
            JsonNode info = pathNode.path("info");
            int totalMinutes = info.path("totalTime").asInt(-1);
            if (totalMinutes < 0) {
                continue;
            }

            Path routePath = new Path(
                    new Minutes(totalMinutes),
                    info.path("transferCount").asInt(0),
                    parseSubPaths(pathNode.path("subPath"))
            );
            candidates.add(new OdsayRouteCandidate(new Minutes(totalMinutes), routePath));
        }

        return candidates.stream()
                .min(Comparator.comparingInt(candidate -> candidate.duration().getValue()));
    }

    public Optional<Minutes> findWalkingTime(
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
        if (response.has("error")) {
            return Optional.empty();
        }

        int pointDistance = response.path("result").path("pointDistance").asInt(-1);
        if (pointDistance >= 0) {
            return Optional.of(estimateWalkingMinutes(pointDistance));
        }

        JsonNode paths = response.path("result").path("path");
        if (!paths.isArray()) {
            return Optional.empty();
        }

        List<Minutes> candidates = new ArrayList<>();
        for (JsonNode pathNode : paths) {
            JsonNode info = pathNode.path("info");
            int totalWalkTime = info.path("totalWalkTime").asInt(-1);
            if (totalWalkTime >= 0) {
                candidates.add(new Minutes(totalWalkTime));
                continue;
            }

            int totalWalkDistance = info.path("totalWalk").asInt(-1);
            if (totalWalkDistance > 0) {
                candidates.add(estimateWalkingMinutes(totalWalkDistance));
                continue;
            }

            int walkingSubPathMinutes = walkingSubPathMinutes(pathNode.path("subPath"));
            if (walkingSubPathMinutes > 0) {
                candidates.add(new Minutes(walkingSubPathMinutes));
            }
        }

        return candidates.stream()
                .min(Comparator.comparingInt(Minutes::getValue));
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

    private List<SubPath> parseSubPaths(JsonNode subPathNodes) {
        if (!subPathNodes.isArray()) {
            return List.of();
        }

        List<SubPath> subPaths = new ArrayList<>();
        for (JsonNode subPathNode : subPathNodes) {
            subPaths.add(new SubPath(
                    subPathNode.path("trafficType").asInt(),
                    new Minutes(subPathNode.path("sectionTime").asInt(0)),
                    text(subPathNode, "startName"),
                    text(subPathNode, "endName"),
                    laneName(subPathNode.path("lane"))
            ));
        }
        return subPaths;
    }

    private int walkingSubPathMinutes(JsonNode subPathNodes) {
        if (!subPathNodes.isArray()) {
            return 0;
        }

        int total = 0;
        for (JsonNode subPathNode : subPathNodes) {
            if (subPathNode.path("trafficType").asInt() == 3) {
                total += subPathNode.path("sectionTime").asInt(0);
            }
        }
        return total;
    }

    private Minutes estimateWalkingMinutes(int distanceMeters) {
        return new Minutes((int) Math.ceil(distanceMeters / WALKING_METERS_PER_MINUTE));
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
