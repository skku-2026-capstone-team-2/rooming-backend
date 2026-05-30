package com.rooming.domain.locations.client;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.RequestMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class ExternalApiTrace {

    private ExternalApiTrace() {
    }

    static RequestMatcher printRequest(String apiName) {
        return request -> {
            System.out.println();
            System.out.println("=== " + apiName + " outbound request ===");
            System.out.println(request.getMethod() + " " + request.getURI());
            System.out.println("headers=" + request.getHeaders());
            String body = body(request);
            if (!body.isBlank()) {
                System.out.println("body=" + body);
            }
        };
    }

    static void printExternalResponse(String apiName, String responseJson) {
        System.out.println("=== " + apiName + " external response ===");
        System.out.println(responseJson);
    }

    static void printMappedResult(String apiName, Object mappedResult) {
        System.out.println("=== " + apiName + " mapped Java result ===");
        System.out.println(mappedResult);
    }

    private static String body(ClientHttpRequest request) throws IOException {
        if (request instanceof MockClientHttpRequest mockRequest) {
            return mockRequest.getBodyAsString(StandardCharsets.UTF_8);
        }
        return "";
    }
}