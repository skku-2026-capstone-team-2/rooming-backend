package com.skku.zip.domain.locations.client;

import com.skku.zip.domain.locations.dto.OdsayRouteCandidate;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OdsayClientContractTest {

    private static final String BASE_URL = "https://odsay.example.test/v1/api";
    private static final String API_KEY = "test-odsay-key";

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
                          "transferCount": 1,
                          "mapObj": "0:0@bus-lane"
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
                            "lane": [{"busNo": "62-1"}],
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

        server.expect(requestToUriTemplate(
                        BASE_URL + "/searchPubTransPathT"
                                + "?SX={startLongitude}&SY={startLatitude}"
                                + "&EX={endLongitude}&EY={endLatitude}"
                                + "&OPT=0&SearchType=0&apiKey={apiKey}",
                        126.9748,
                        37.2945,
                        126.9718,
                        37.2961,
                        API_KEY
                ))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("SX", "126.9748"))
                .andExpect(queryParam("SY", "37.2945"))
                .andExpect(queryParam("EX", "126.9718"))
                .andExpect(queryParam("EY", "37.2961"))
                .andExpect(queryParam("OPT", "0"))
                .andExpect(queryParam("SearchType", "0"))
                .andExpect(queryParam("apiKey", API_KEY))
                .andExpect(ExternalApiTrace.printRequest("ODSAY public transport route"))
                .andRespond(withSuccess(odsayResponse, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/loadLane?mapObject=0%3A0%40bus-lane&apiKey=" + API_KEY))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("mapObject", "0%3A0%40bus-lane"))
                .andExpect(queryParam("apiKey", API_KEY))
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
        assertThat(candidate.path().getSubPaths().get(1).getLaneName()).isEqualTo("62-1");
        assertThat(candidate.path().getSubPaths().get(1).getDistanceMeters()).isEqualTo(1570);
        assertThat(candidate.path().getSubPaths().get(1).getPoints()).hasSize(3);
        assertThat(candidate.path().getSubPaths().get(1).getPoints().get(1).getLatitude()).isEqualTo(37.2959);
        assertThat(candidate.path().getSubPaths().get(1).getPoints().get(2).getLatitude()).isEqualTo(37.2961);
    }
}
