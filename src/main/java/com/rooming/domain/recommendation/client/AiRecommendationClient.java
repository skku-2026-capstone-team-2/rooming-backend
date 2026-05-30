package com.rooming.domain.recommendation.client;

import com.rooming.common.exception.UpstreamServiceException;
import com.rooming.domain.recommendation.dto.AiRecommendationDtos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
@Slf4j
public class AiRecommendationClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);
    private static final int DEFAULT_HTTP_PORT = 8000;
    private static final String DEFAULT_BASE_URL = "http://localhost:" + DEFAULT_HTTP_PORT;

    @Value("${rooming.ai.base-url:http://localhost:8000}")
    private String baseUrl;

    private final RestClient restClient;

    @Autowired
    public AiRecommendationClient(RestClient.Builder restClientBuilder) {
        this(restClientBuilder
                .requestFactory(requestFactory())
                .build());
    }

    AiRecommendationClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public AiRecommendationDtos.Response recommend(AiRecommendationDtos.Request request) {
        String url = recommendUrl();
        try {
            AiRecommendationDtos.Response response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiRecommendationDtos.Response.class);

            if (response == null) {
                throw new UpstreamServiceException("AI recommendation service returned an empty response.");
            }
            return response;
        } catch (RestClientResponseException exception) {
            log.warn(
                    "AI recommendation service returned HTTP {} from {}: {}",
                    exception.getStatusCode(),
                    url,
                    exception.getResponseBodyAsString(),
                    exception
            );
            throw new UpstreamServiceException(
                    "AI recommendation service returned HTTP " + exception.getStatusCode().value() + ".",
                    exception
            );
        } catch (RestClientException exception) {
            log.warn("AI recommendation service request failed for {}: {}", url, exception.getMessage(), exception);
            throw new UpstreamServiceException(
                    "AI recommendation service request failed: " + rootMessage(exception),
                    exception
            );
        }
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return requestFactory;
    }

    private String recommendUrl() {
        String normalizedBaseUrl = baseUrl == null || baseUrl.isBlank()
                ? DEFAULT_BASE_URL
                : baseUrl.trim();
        if (!normalizedBaseUrl.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            normalizedBaseUrl = "http://" + normalizedBaseUrl;
        }
        normalizedBaseUrl = applyDefaultHttpPort(normalizedBaseUrl);
        return normalizedBaseUrl.replaceAll("/+$", "") + "/recommend";
    }

    private String applyDefaultHttpPort(String normalizedBaseUrl) {
        String httpScheme = "http://";
        if (!normalizedBaseUrl.regionMatches(true, 0, httpScheme, 0, httpScheme.length())) {
            return normalizedBaseUrl;
        }

        String withoutScheme = normalizedBaseUrl.substring(httpScheme.length());
        int pathStart = withoutScheme.indexOf('/');
        String authority = pathStart < 0 ? withoutScheme : withoutScheme.substring(0, pathStart);
        if (authority.contains(":")) {
            return normalizedBaseUrl;
        }

        String path = pathStart < 0 ? "" : withoutScheme.substring(pathStart);
        return httpScheme + authority + ":" + DEFAULT_HTTP_PORT + path;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null || current.getMessage().isBlank()
                ? throwable.getMessage()
                : current.getMessage();
    }
}