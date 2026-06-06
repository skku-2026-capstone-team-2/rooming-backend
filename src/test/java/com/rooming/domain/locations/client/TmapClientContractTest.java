package com.rooming.domain.locations.client;

import com.rooming.domain.locations.dto.OdsayRouteCandidate;
import com.rooming.domain.locations.dto.TmapInfrastructureCandidate;
import com.rooming.domain.locations.entity.type.INFRA_CATEGORY;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TmapClientContractTest {

    private static final String BASE_URL = "https://tmap.example.test/tmap";
    private static final String API_KEY = "test-tmap-key";

    private MockRestServiceServer server;
    private TmapClient tmapClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        tmapClient = new TmapClient(restClientBuilder);
        ReflectionTestUtils.setField(tmapClient, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(tmapClient, "apiKey", API_KEY);
    }

    @AfterEach
    void verifyExternalCalls() {
        server.verify();
    }

    @Test
    void infrastructureSearchSkipsEtcCategoryWithoutTmapKeyword() {
        String emptyTmapResponse = """
                {
                  "searchPoiInfo": {
                    "pois": {
                      "poi": []
                    }
                  }
                }
                """;

        server.expect(ExpectedCount.times(INFRA_CATEGORY.values().length - 1), request -> {
                    assertThat(request.getURI().getQuery())
                            .contains("count=2")
                            .doesNotContain("categories=null");
                })
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("appKey", API_KEY))
                .andRespond(withSuccess(emptyTmapResponse, MediaType.APPLICATION_JSON));

        List<TmapInfrastructureCandidate> candidates = tmapClient.findInfrastructureCandidates(
                37.2945,
                126.9748,
                2
        );

        assertThat(candidates).isEmpty();
    }

    @Test
    void infrastructureSearchReportsQuotaExceededOnTmapLimitResponse() {
        server.expect(ExpectedCount.once(), request -> {
                    assertThat(request.getURI().getQuery())
                            .contains("count=2")
                            .doesNotContain("categories=null");
                })
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("appKey", API_KEY))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("quota exceeded")
                        .contentType(MediaType.TEXT_PLAIN));

        assertThat(tmapClient.findInfrastructureCandidatesWithQuotaStatus(37.2945, 126.9748, 1).quotaExceeded())
                .isTrue();
    }

    @Test
    void walkingRouteShowsTmapRequestBodyAndMappedResponse() {
        String tmapResponse = """
                {
                  "features": [
                    {
                      "properties": {
                        "totalTime": 360,
                        "totalDistance": 420,
                        "time": 120,
                        "distance": 150,
                        "name": "Campus Walkway",
                        "description": "Walk straight"
                      },
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [[126.9718, 37.2961], [126.9721, 37.2958]]
                      }
                    },
                    {
                      "properties": {
                        "time": 240,
                        "distance": 270,
                        "roadName": "Seobu-ro"
                      },
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [[126.9721, 37.2958], [126.9748, 37.2945]]
                      }
                    }
                  ]
                }
                """;

        server.expect(requestToUriTemplate(BASE_URL + "/routes/pedestrian?version=1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("appKey", API_KEY))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "startX": 126.9718,
                          "startY": 37.2961,
                          "endX": 126.9748,
                          "endY": 37.2945,
                          "reqCoordType": "WGS84GEO",
                          "resCoordType": "WGS84GEO",
                          "startName": "start",
                          "endName": "end",
                          "searchOption": "0",
                          "sort": "index"
                        }
                        """))
                .andExpect(ExternalApiTrace.printRequest("TMAP walking route"))
                .andRespond(withSuccess(tmapResponse, MediaType.APPLICATION_JSON));

        OdsayRouteCandidate candidate = tmapClient.findWalkingRoute(
                37.2961,
                126.9718,
                37.2945,
                126.9748
        ).orElseThrow();

        ExternalApiTrace.printExternalResponse("TMAP walking route", tmapResponse);
        ExternalApiTrace.printMappedResult("TMAP walking route", candidate);

        assertThat(candidate.duration().getValue()).isEqualTo(6);
        assertThat(candidate.path().getTransferCount()).isZero();
        assertThat(candidate.path().getSubPaths()).hasSize(2);
        assertThat(candidate.path().getSubPaths().getFirst().getLaneName()).isEqualTo("Campus Walkway");
        assertThat(candidate.path().getSubPaths().getFirst().getPoints()).hasSize(2);
    }

    @Test
    void walkingRouteReportsQuotaExceededOnTmapLimitResponse() {
        server.expect(requestToUriTemplate(BASE_URL + "/routes/pedestrian?version=1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("appKey", API_KEY))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("quota exceeded")
                        .contentType(MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> tmapClient.findWalkingRoute(
                37.2961,
                126.9718,
                37.2945,
                126.9748
        )).isInstanceOf(TmapQuotaExceededException.class);
    }
}
