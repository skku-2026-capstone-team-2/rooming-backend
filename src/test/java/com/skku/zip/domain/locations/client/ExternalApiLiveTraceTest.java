package com.skku.zip.domain.locations.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skku.zip.domain.locations.dto.OdsayRouteCandidate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@EnabledIfSystemProperty(named = "rooming.live-external-api", matches = "true")
class ExternalApiLiveTraceTest {

    private static final String TMAP_BASE_URL = "https://apis.openapi.sk.com/tmap";
    private static final String ODSAY_BASE_URL = "https://api.odsay.com/v1/api";
    private static final double SKKU_LONGITUDE = 126.9748;
    private static final double SKKU_LATITUDE = 37.2945;
    private static final double PROPERTY_LONGITUDE = 126.9718;
    private static final double PROPERTY_LATITUDE = 37.2961;
    private static final double ODSAY_GUIDE_START_LONGITUDE = 126.9027279;
    private static final double ODSAY_GUIDE_START_LATITUDE = 37.5349277;
    private static final double ODSAY_GUIDE_END_LONGITUDE = 126.9145430;
    private static final double ODSAY_GUIDE_END_LATITUDE = 37.5499421;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void tmapSendsRealWalkingRouteResponse() throws Exception {
        String apiKey = apiKey("TMAP_API_KEY");

        Map<String, Object> walkingRouteRequest = new LinkedHashMap<>();
        walkingRouteRequest.put("startX", PROPERTY_LONGITUDE);
        walkingRouteRequest.put("startY", PROPERTY_LATITUDE);
        walkingRouteRequest.put("endX", SKKU_LONGITUDE);
        walkingRouteRequest.put("endY", SKKU_LATITUDE);
        walkingRouteRequest.put("reqCoordType", "WGS84GEO");
        walkingRouteRequest.put("resCoordType", "WGS84GEO");
        walkingRouteRequest.put("startName", "start");
        walkingRouteRequest.put("endName", "end");
        walkingRouteRequest.put("searchOption", "0");
        walkingRouteRequest.put("sort", "index");

        String walkingRouteResponse = restClient.post()
                .uri(TMAP_BASE_URL + "/routes/pedestrian?version=1")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("appKey", apiKey)
                .body(walkingRouteRequest)
                .retrieve()
                .body(String.class);

        JsonNode walkingRouteJson = printJsonResponse(
                "Real TMAP walking route",
                "POST /tmap/routes/pedestrian?version=1 appKey=<redacted>\n"
                        + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(walkingRouteRequest),
                walkingRouteResponse
        );
        assertThat(walkingRouteJson.path("features").isArray()).isTrue();
    }

    @Test
    void odsaySendsRealPublicTransportAndRouteGraphicResponses() throws Exception {
        String apiKey = apiKey("ODSAY_API_KEY");

        String routeResponse = restClient.get()
                .uri(odsayRouteUri(apiKey))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

        JsonNode routeJson = printJsonResponse(
                "Real ODSAY public transport route",
                "GET /v1/api/searchPubTransPathT?SX=" + ODSAY_GUIDE_START_LONGITUDE
                        + "&SY=" + ODSAY_GUIDE_START_LATITUDE
                        + "&EX=" + ODSAY_GUIDE_END_LONGITUDE
                        + "&EY=" + ODSAY_GUIDE_END_LATITUDE
                        + "&apiKey=<redacted>",
                routeResponse
        );
        assumeNoOdsayProviderError("public transport route", routeJson);
        assertThat(hasOdsayPaths(routeJson))
                .as("ODSAY live route response must contain result.path. Provider response: %s", routeJson)
                .isTrue();

        String mapObject = firstMapObject(routeJson);
        assumeTrue(mapObject != null, "ODSAY response had no route graphic mapObj.");

        String routeGraphicResponse = restClient.get()
                .uri(odsayRouteGraphicUri(mapObject, apiKey))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

        JsonNode routeGraphicJson = printJsonResponse(
                "Real ODSAY route graphic",
                "GET /v1/api/loadLane?mapObject=" + routeGraphicMapObject(mapObject) + "&apiKey=<redacted>",
                routeGraphicResponse
        );
        assumeNoOdsayProviderError("route graphic", routeGraphicJson);
        assertThat(routeGraphicJson.path("result").path("lane").isArray()).isTrue();

        OdsayRouteCandidate parsedRoute = liveOdsayClient(apiKey)
                .findFastestRoute(
                        ODSAY_GUIDE_START_LATITUDE,
                        ODSAY_GUIDE_START_LONGITUDE,
                        ODSAY_GUIDE_END_LATITUDE,
                        ODSAY_GUIDE_END_LONGITUDE
                )
                .orElseThrow();
        ExternalApiTrace.printMappedResult("Real ODSAY public transport route", parsedRoute);
        assertThat(parsedRoute.duration().getValue()).isPositive();
        assertThat(parsedRoute.path().getSubPaths()).isNotEmpty();
        assertThat(parsedRoute.path().getSubPaths().stream()
                .filter(subPath -> subPath.getTrafficType() == 1 || subPath.getTrafficType() == 2)
                .flatMap(subPath -> subPath.getPoints().stream()))
                .isNotEmpty();
    }

    private JsonNode printJsonResponse(String title, String request, String responseBody) throws IOException {
        assertThat(responseBody).as(title + " response body").isNotBlank();

        JsonNode responseJson = objectMapper.readTree(responseBody);
        System.out.println();
        System.out.println("=== " + title + " request ===");
        System.out.println(request);
        System.out.println("=== " + title + " provider response ===");
        System.out.println(responseJson.toPrettyString());
        return responseJson;
    }

    private String firstMapObject(JsonNode routeJson) {
        JsonNode paths = routeJson.path("result").path("path");
        if (!paths.isArray()) {
            return null;
        }
        for (JsonNode path : paths) {
            String mapObject = path.path("info").path("mapObj").asText(null);
            if (mapObject != null && !mapObject.isBlank()) {
                return mapObject;
            }
        }
        return null;
    }

    private boolean hasOdsayPaths(JsonNode routeJson) {
        return routeJson.path("result").path("path").isArray();
    }

    private void assumeNoOdsayProviderError(String requestName, JsonNode responseJson) {
        assumeTrue(
                !responseJson.has("error"),
                "ODSAY " + requestName + " returned a provider error. "
                        + "Inspect the printed response and check the ODSAY API key or provider account settings."
        );
    }

    private String routeGraphicMapObject(String mapObject) {
        return "0:0@" + mapObject;
    }

    private URI odsayRouteUri(String apiKey) {
        return URI.create(ODSAY_BASE_URL + "/searchPubTransPathT"
                + "?SX=" + ODSAY_GUIDE_START_LONGITUDE
                + "&SY=" + ODSAY_GUIDE_START_LATITUDE
                + "&EX=" + ODSAY_GUIDE_END_LONGITUDE
                + "&EY=" + ODSAY_GUIDE_END_LATITUDE
                + "&apiKey=" + queryValue(apiKey));
    }

    private URI odsayRouteGraphicUri(String mapObject, String apiKey) {
        return URI.create(ODSAY_BASE_URL + "/loadLane"
                + "?mapObject=" + queryValue(routeGraphicMapObject(mapObject))
                + "&apiKey=" + queryValue(apiKey));
    }

    private String queryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String apiKey(String name) throws IOException {
        String apiKey = firstNonBlank(System.getProperty(name), System.getenv(name), envFileValue(name));
        assumeTrue(apiKey != null, name + " is required for live external API tracing.");
        return apiKey;
    }

    private OdsayClient liveOdsayClient(String apiKey) {
        OdsayClient odsayClient = new OdsayClient(RestClient.builder());
        ReflectionTestUtils.setField(odsayClient, "baseUrl", ODSAY_BASE_URL);
        ReflectionTestUtils.setField(odsayClient, "apiKey", apiKey);
        return odsayClient;
    }

    private String envFileValue(String name) throws IOException {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return null;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(envPath)) {
            properties.load(reader);
        }
        return properties.getProperty(name);
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
