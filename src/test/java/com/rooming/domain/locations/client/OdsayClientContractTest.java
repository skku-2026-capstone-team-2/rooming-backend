package com.rooming.domain.locations.client;

import com.rooming.domain.locations.dto.OdsayRouteCandidate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OdsayClientContractTest {

    private static final String BASE_URL = "https://odsay.example.test/v1/api";
    private static final String API_KEY = "test+odsay-key";

    private MockRestServiceServer server;
    private OdsayClient odsayClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        odsayClient = new OdsayClient(restClientBuilder);
        ReflectionTestUtils.setField(odsayClient, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(odsayClient, "apiKey", API_KEY);
    }

    @AfterEach
    void verifyExternalCalls() {
        server.verify();
    }

    @Test
    void publicTransportRouteShowsOdsayQueryAndMappedResponse() {
        String odsayResponse = """
                {
                  "result": {
                    "path": [
                      {
                        "info": {
                          "totalTime": 18,
                          "busTransitCount": 1,
                          "subwayTransitCount": 1,
                          "mapObj": "bus-lane"
                        },
                        "subPath": [
                          {
                            "trafficType": 3,
                            "distance": 250.2,
                            "sectionTime": 4,
                            "startName": "Target place",
                            "endName": "Bus stop",
                            "startX": 126.9748,
                            "startY": 37.2945,
                            "endX": 126.9739,
                            "endY": 37.2950
                          },
                          {
                            "trafficType": 2,
                            "distance": 1570,
                            "sectionTime": 14,
                            "startName": "Bus stop",
                            "endName": "Property stop",
                            "lane": {"name": "Line 2"},
                            "passStopList": {
                              "stations": [
                                {"x": "126.9739", "y": "37.2950"},
                                {"x": "126.9727", "y": "37.2956"},
                                {"x": "126.9718", "y": "37.2961"}
                              ]
                            }
                          }
                        ]
                      },
                      {
                        "info": {
                          "totalTime": 31,
                          "transferCount": 0
                        },
                        "subPath": []
                      }
                    ]
                  }
                }
                """;
        String odsayGraphicResponse = """
                {
                  "result": {
                    "lane": [
                      {
                        "section": [
                          {
                            "graphPos": [
                              {"x": 126.9739, "y": 37.2950},
                              {"x": 126.9730, "y": 37.2959},
                              {"x": 126.9718, "y": 37.2961}
                            ]
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        server.expect(requestTo(BASE_URL + "/searchPubTransPathT"
                        + "?SX=126.9748&SY=37.2945"
                        + "&EX=126.9718&EY=37.2961"
                        + "&apiKey=test%2Bodsay-key"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("SX", "126.9748"))
                .andExpect(queryParam("SY", "37.2945"))
                .andExpect(queryParam("EX", "126.9718"))
                .andExpect(queryParam("EY", "37.2961"))
                .andExpect(queryParam("apiKey", "test%2Bodsay-key"))
                .andExpect(ExternalApiTrace.printRequest("ODSAY public transport route"))
                .andRespond(withSuccess(odsayResponse, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL
                        + "/loadLane?mapObject=0%3A0%40bus-lane&apiKey=test%2Bodsay-key"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("mapObject", "0%3A0%40bus-lane"))
                .andExpect(queryParam("apiKey", "test%2Bodsay-key"))
                .andExpect(ExternalApiTrace.printRequest("ODSAY route graphic"))
                .andRespond(withSuccess(odsayGraphicResponse, MediaType.APPLICATION_JSON));

        OdsayRouteCandidate candidate = odsayClient.findFastestRoute(
                37.2945,
                126.9748,
                37.2961,
                126.9718
        ).orElseThrow();

        ExternalApiTrace.printExternalResponse("ODSAY public transport route", odsayResponse);
        ExternalApiTrace.printExternalResponse("ODSAY route graphic", odsayGraphicResponse);
        ExternalApiTrace.printMappedResult("ODSAY public transport route", candidate);

        assertThat(candidate.duration().getValue()).isEqualTo(18);
        assertThat(candidate.path().getTransferCount()).isEqualTo(1);
        assertThat(candidate.path().getSubPaths()).hasSize(2);
        assertThat(candidate.path().getSubPaths().get(0).getDistanceMeters()).isEqualTo(250);
        assertThat(candidate.path().getSubPaths().get(0).getPoints()).hasSize(2);
        assertThat(candidate.path().getSubPaths().get(1).getLaneName()).isEqualTo("Line 2");
        assertThat(candidate.path().getSubPaths().get(1).getDistanceMeters()).isEqualTo(1570);
        assertThat(candidate.path().getSubPaths().get(1).getPoints()).hasSize(3);
        assertThat(candidate.path().getSubPaths().get(1).getPoints().get(1).getLatitude()).isEqualTo(37.2959);
        assertThat(candidate.path().getSubPaths().get(1).getPoints().get(2).getLatitude()).isEqualTo(37.2961);
    }
}