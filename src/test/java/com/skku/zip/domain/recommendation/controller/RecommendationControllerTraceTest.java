package com.skku.zip.domain.recommendation.controller;

import com.skku.zip.domain.locations.dto.CoordinateDto;
import com.skku.zip.domain.recommendation.dto.RecommendationDtos;
import com.skku.zip.domain.recommendation.service.RecommendationService;
import com.skku.zip.domain.seeker.entity.Seeker;
import com.skku.zip.security.principal.PrincipalDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecommendationControllerTraceTest {

    private RecommendationService recommendationService;
    private MockMvc mockMvc;
    private Seeker seeker;

    @BeforeEach
    void setUp() {
        recommendationService = mock(RecommendationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RecommendationController(recommendationService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        seeker = Seeker.builder()
                .name("Recommendation trace seeker")
                .email("trace@example.test")
                .provider("google")
                .loginId("trace-login")
                .build();
        PrincipalDetails principalDetails = new PrincipalDetails(seeker, Map.of("name", seeker.getName()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken(
                principalDetails,
                null,
                principalDetails.getAuthorities()
        ));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recommendationEndpointShowsFrontendRequestAndFrontendResponse() throws Exception {
        String frontendRequestJson = """
                {
                  "query": "Find a quiet studio near campus.",
                  "preferences": ["quiet", "low maintenance fee"],
                  "topN": 3
                }
                """;
        when(recommendationService.recommend(any(Seeker.class), any(RecommendationDtos.Request.class)))
                .thenReturn(recommendationData());

        MvcResult result = mockMvc.perform(post("/api/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(frontendRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].recommendationId").value(7001))
                .andExpect(jsonPath("$.data.results[0].propertyId").value(101))
                .andExpect(jsonPath("$.data.results[0].property.location.latitude").value(37.2945))
                .andExpect(jsonPath("$.data.results[0].property.depositAmount").value(500))
                .andExpect(jsonPath("$.data.results[0].firstTargetPlaceRoute.targetPlaceId").value(29))
                .andExpect(jsonPath("$.data.results[0].firstTargetPlaceRoute.durationMinutes").value(18))
                .andExpect(jsonPath("$.data.results[0].firstTargetPlaceRoute.routeJson.pathList[1].lane")
                        .value("62-1"))
                .andExpect(jsonPath("$.data.results[0].firstTargetPlaceRoute.routeJson.pathList[1].points[2].latitude")
                        .value(37.2961))
                .andExpect(jsonPath("$.data.results[0].infrastructures[0].infrastructureId").value(10))
                .andExpect(jsonPath("$.data.results[0].infrastructures[0].location.latitude").value(37.2950))
                .andExpect(jsonPath("$.data.results[0].infrastructures[0].walkingMinutes").value(4))
                .andExpect(jsonPath("$.data.results[0].infrastructures[2].walkingMinutes").value(9))
                .andExpect(jsonPath("$.data.results[0].explanation")
                        .value("Quiet studio with a short target-place route."))
                .andReturn();

        System.out.println();
        System.out.println("=== Frontend -> Java recommendation API request ===");
        System.out.println("POST /api/v1/recommendations");
        System.out.println(frontendRequestJson);
        System.out.println("=== Java recommendation API -> frontend response ===");
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    void seekerCanDeleteSavedRecommendation() throws Exception {
        mockMvc.perform(delete("/api/v1/recommendations/{recommendationId}", 7001L))
                .andExpect(status().isNoContent());

        verify(recommendationService).deleteRecommendation(seeker, 7001L);
    }

    private RecommendationDtos.Data recommendationData() {
        return new RecommendationDtos.Data(
                "Recommendation completed.",
                List.of(new RecommendationDtos.Result(
                        7001L,
                        101L,
                        false,
                        new RecommendationDtos.PropertyDetails(
                                new CoordinateDto(37.2945, 126.9748),
                                500,
                                55,
                                5
                        ),
                        new RecommendationDtos.TargetPlaceRoute(
                                29L,
                                "PUBLIC_TRANSPORT",
                                18,
                                routeJson()
                        ),
                        List.of(
                                infrastructure(10L, "Campus pharmacy", "PHARMACY", 37.2950, 126.9750, 4),
                                infrastructure(11L, "Convenience store", "CONVENIENT_STORE", 37.2942, 126.9743, 7),
                                infrastructure(16L, "Cafe", "CAFE", 37.2940, 126.9741, 9)
                        ),
                        "Quiet studio with a short target-place route."
                ))
        );
    }

    private Map<String, Object> routeJson() {
        return Map.of(
                "totalTime", 18,
                "transferCount", 1,
                "pathList", List.of(
                        Map.of(
                                "type", "WALK",
                                "time", 4,
                                "distance", 250,
                                "points", List.of(
                                        new CoordinateDto(37.2945, 126.9748),
                                        new CoordinateDto(37.2950, 126.9739)
                                )
                        ),
                        Map.of(
                                "type", "BUS",
                                "time", 14,
                                "lane", "62-1",
                                "distance", 1570,
                                "points", List.of(
                                        new CoordinateDto(37.2950, 126.9739),
                                        new CoordinateDto(37.2956, 126.9727),
                                        new CoordinateDto(37.2961, 126.9718)
                                )
                        )
                )
        );
    }

    private RecommendationDtos.InfrastructureDetails infrastructure(
            Long id,
            String name,
            String category,
            double latitude,
            double longitude,
            int walkingMinutes
    ) {
        return new RecommendationDtos.InfrastructureDetails(
                id,
                name,
                category,
                id + " Infra-ro",
                new CoordinateDto(latitude, longitude),
                walkingMinutes
        );
    }
}
