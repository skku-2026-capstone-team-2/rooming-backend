package com.skku.zip.domain.recommendation.client;

import com.skku.zip.domain.recommendation.dto.AiRecommendationDtos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiRecommendationClientContractTest {

    private static final String BASE_URL = "https://ai.example.test";

    private MockRestServiceServer server;
    private AiRecommendationClient aiRecommendationClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        aiRecommendationClient = new AiRecommendationClient(restClientBuilder);
        ReflectionTestUtils.setField(aiRecommendationClient, "baseUrl", BASE_URL);
    }

    @AfterEach
    void verifyExternalCalls() {
        server.verify();
    }

    @Test
    void recommendationShowsAiRequestAndAiResponse() {
        AiRecommendationDtos.Request request = new AiRecommendationDtos.Request(
                "Find a quiet studio near campus.",
                List.of("quiet", "low maintenance fee"),
                7L,
                3
        );
        String aiResponseJson = """
                {
                  "success": true,
                  "message": "Recommendation completed.",
                  "results": [
                    {
                      "property_id": 101,
                      "infra_ids": [10, 11, 16],
                      "explanation": "Quiet studio with a short target-place route."
                    },
                    {
                      "property_id": 103,
                      "infra_ids": [18],
                      "explanation": "Lower maintenance fee among nearby options."
                    }
                  ]
                }
                """;

        server.expect(requestTo(BASE_URL + "/recommend"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "query": "Find a quiet studio near campus.",
                          "preferences": ["quiet", "low maintenance fee"],
                          "seeker_id": 7,
                          "top_n": 3
                        }
                        """))
                .andExpect(printRequest("Java backend -> AI recommendation backend"))
                .andRespond(withSuccess(aiResponseJson, MediaType.APPLICATION_JSON));

        AiRecommendationDtos.Response response = aiRecommendationClient.recommend(request);

        printJson("AI recommendation backend -> Java backend response", aiResponseJson);
        printObject("Mapped AiRecommendationDtos.Response", response);

        assertThat(response.success()).isTrue();
        assertThat(response.results()).extracting(AiRecommendationDtos.Result::propertyId)
                .containsExactly(101L, 103L);
        assertThat(response.results().getFirst().infraIds()).containsExactly(10L, 11L, 16L);
        assertThat(response.results().getFirst().explanation())
                .isEqualTo("Quiet studio with a short target-place route.");
    }

    private RequestMatcher printRequest(String label) {
        return request -> {
            System.out.println();
            System.out.println("=== " + label + " ===");
            System.out.println(request.getMethod() + " " + request.getURI());
            System.out.println("headers=" + request.getHeaders());
            String body = requestBody(request);
            if (!body.isBlank()) {
                System.out.println("body=" + body);
            }
        };
    }

    private String requestBody(ClientHttpRequest request) throws IOException {
        if (request instanceof MockClientHttpRequest mockRequest) {
            return mockRequest.getBodyAsString(StandardCharsets.UTF_8);
        }
        return "";
    }

    private void printJson(String label, String json) {
        System.out.println("=== " + label + " ===");
        System.out.println(json);
    }

    private void printObject(String label, Object value) {
        System.out.println("=== " + label + " ===");
        System.out.println(value);
    }
}
