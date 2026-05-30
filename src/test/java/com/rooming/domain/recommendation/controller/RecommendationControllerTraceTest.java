package com.rooming.domain.recommendation.controller;

import com.rooming.domain.locations.dto.CoordinateDto;
import com.rooming.domain.locations.dto.RouteGeometryDetail;
import com.rooming.domain.property.entity.TradeType;
import com.rooming.domain.recommendation.dto.RecommendationDtos;
import com.rooming.domain.recommendation.service.RecommendationService;
import com.rooming.domain.seeker.entity.Seeker;
import com.rooming.security.principal.PrincipalDetails;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecommendationControllerTraceTest {

    private RecommendationService recommendationService;
    private MockMvc mockMvc;
    private Seeker seeker;

    @BeforeEach
    void dsetUp() {
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
                .andExpect(jsonPath("$.data.results[0].property.tradeType").value("MONTHLY_RENT"))
                .andExpect(jsonPath("$.data.results[0].property.depositAmount").value(500))
                .andExpect(jsonPath("$.data.results[0].property.description").value("Quiet studio near campus."))
                .andExpect(jsonPath("$.data.results[0].property.tags[0]").value("quiet"))
                .andExpect(jsonPath("$.data.results[0].firstTargetPlaceRoute.targetPlaceId").value(29))
                .andExpect(jsonPath("$.data.results[0].firstTargetPlaceRoute.durationMinutes").value(18))
                .andExpect(jsonPath("$.data.results[0].firstTargetPlaceRoute.transferCount").value(1))
                .andExpect(jsonPath("$.data.results[0].firstTargetPlaceRoute.subPaths[1].lane")
                        .value("62-1"))
                .andExpect(jsonPath("$.data.results[0].firstTargetPlaceRoute.routeJson").doesNotExist())
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
    void recommendationRouteEndpointShowsMapGeometryOnlyWhenRequested() throws Exception {
        when(recommendationService.getRecommendationRoute(seeker, 7001L, RouteGeometryDetail.DETAIL))
                .thenReturn(routeDetailData());

        MvcResult result = mockMvc.perform(get("/api/v1/recommendations/{recommendationId}/route", 7001L)
                        .param("detail", "DETAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.detail").value("DETAIL"))
                .andExpect(jsonPath("$.data.path.totalPointCount").value(5))
                .andExpect(jsonPath("$.data.path.pathList[1].points[2].latitude").value(37.2961))
                .andReturn();

        System.out.println();
        System.out.println("=== Frontend -> Java recommendation route request ===");
        System.out.println("GET /api/v1/recommendations/7001/route?detail=DETAIL");
        System.out.println("=== Java recommendation route API -> frontend response ===");
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
                                TradeType.MONTHLY_RENT,
                                500,
                                55,
                                5,
                                "Quiet studio near campus.",
                                List.of("quiet", "campus")
                        ),
                        new RecommendationDtos.TargetPlaceRoute(
                                29L,
                                "PUBLIC_TRANSPORT",
                                18,
                                1,
                                routeSubPathSummaries()
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

    private RecommendationDtos.RouteDetailData routeDetailData() {
        return new RecommendationDtos.RouteDetailData(
                7001L,
                101L,
                29L,
                "PUBLIC_TRANSPORT",
                18,
                RouteGeometryDetail.DETAIL,
                new RecommendationDtos.RoutePath(
                        18,
                        1,
                        5,
                        List.of(
                                new RecommendationDtos.RouteSubPathDetail(
                                        "WALK",
                                        3,
                                        4,
                                        "Target place",
                                        "Bus stop",
                                        null,
                                        250,
                                        null,
                                        List.of(
                                                new CoordinateDto(37.2945, 126.9748),
                                                new CoordinateDto(37.2950, 126.9739)
                                        )
                                ),
                                new RecommendationDtos.RouteSubPathDetail(
                                        "BUS",
                                        2,
                                        14,
                                        "Bus stop",
                                        "Property stop",
                                        "62-1",
                                        1570,
                                        null,
                                        List.of(
                                                new CoordinateDto(37.2950, 126.9739),
                                                new CoordinateDto(37.2956, 126.9727),
                                                new CoordinateDto(37.2961, 126.9718)
                                        )
                                )
                        )
                )
        );
    }

    private List<RecommendationDtos.RouteSubPathSummary> routeSubPathSummaries() {
        return List.of(
                new RecommendationDtos.RouteSubPathSummary(
                        "WALK",
                        3,
                        4,
                        "Target place",
                        "Bus stop",
                        null,
                        250,
                        null
                ),
                new RecommendationDtos.RouteSubPathSummary(
                        "BUS",
                        2,
                        14,
                        "Bus stop",
                        "Property stop",
                        "62-1",
                        1570,
                        null
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