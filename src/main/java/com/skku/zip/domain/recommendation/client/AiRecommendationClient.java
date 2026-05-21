package com.skku.zip.domain.recommendation.client;

import com.skku.zip.common.exception.UpstreamServiceException;
import com.skku.zip.domain.recommendation.dto.AiRecommendationDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AiRecommendationClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:8001";

    @Value("${rooming.ai.base-url:http://localhost:8001}")
    private String baseUrl;

    private final RestClient restClient;

    public AiRecommendationClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public AiRecommendationDtos.Response recommend(AiRecommendationDtos.Request request) {
        try {
            AiRecommendationDtos.Response response = restClient.post()
                    .uri(recommendUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiRecommendationDtos.Response.class);

            if (response == null) {
                throw new UpstreamServiceException("AI recommendation service returned an empty response.");
            }
            return response;
        } catch (RestClientException exception) {
            throw new UpstreamServiceException("AI recommendation service request failed.");
        }
    }

    private String recommendUrl() {
        String normalizedBaseUrl = baseUrl == null || baseUrl.isBlank()
                ? DEFAULT_BASE_URL
                : baseUrl.trim();
        return normalizedBaseUrl.replaceAll("/+$", "") + "/recommend";
    }
}
