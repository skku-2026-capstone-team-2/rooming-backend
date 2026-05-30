package com.skku.zip.domain.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skku.zip.domain.locations.entity.model.InfraAccessibility;
import com.skku.zip.domain.locations.entity.model.Infrastructure;
import com.skku.zip.domain.locations.entity.model.Route;
import com.skku.zip.domain.locations.entity.model.TargetPlace;
import com.skku.zip.domain.locations.entity.type.INFRA_CATEGORY;
import com.skku.zip.domain.locations.entity.type.PLACE_CATEGORY;
import com.skku.zip.domain.locations.entity.type.TRANSPORT_MODE;
import com.skku.zip.domain.locations.entity.value.Minutes;
import com.skku.zip.domain.locations.entity.value.Path;
import com.skku.zip.domain.locations.entity.value.RoadAddress;
import com.skku.zip.domain.locations.entity.value.SubPath;
import com.skku.zip.domain.locations.repository.InfraAccessibilityRepository;
import com.skku.zip.domain.locations.repository.InfrastructureRepository;
import com.skku.zip.domain.locations.repository.RouteRepository;
import com.skku.zip.domain.locations.service.RouteGeometryService;
import com.skku.zip.domain.property.entity.Property;
import com.skku.zip.domain.property.entity.TradeType;
import com.skku.zip.domain.property.repository.PropertyRepository;
import com.skku.zip.domain.recommendation.client.AiRecommendationClient;
import com.skku.zip.domain.recommendation.dto.AiRecommendationDtos;
import com.skku.zip.domain.recommendation.dto.RecommendationDtos;
import com.skku.zip.domain.recommendation.entity.Recommendation;
import com.skku.zip.domain.recommendation.repository.RecommendationRepository;
import com.skku.zip.domain.seeker.entity.Seeker;
import com.skku.zip.domain.seeker.repository.SeekerRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    private final AiRecommendationClient aiRecommendationClient = mock(AiRecommendationClient.class);
    private final SeekerRepository seekerRepository = mock(SeekerRepository.class);
    private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
    private final RouteRepository routeRepository = mock(RouteRepository.class);
    private final RouteGeometryService routeGeometryService = new RouteGeometryService();
    private final InfraAccessibilityRepository infraAccessibilityRepository = mock(InfraAccessibilityRepository.class);
    private final InfrastructureRepository infrastructureRepository = mock(InfrastructureRepository.class);
    private final RecommendationRepository recommendationRepository = mock(RecommendationRepository.class);
    private final RecommendationService recommendationService = new RecommendationService(
            aiRecommendationClient,
            seekerRepository,
            propertyRepository,
            routeRepository,
            routeGeometryService,
            infraAccessibilityRepository,
            infrastructureRepository,
            recommendationRepository,
            new ObjectMapper()
    );

    @Test
    void recommendEnrichesAiIdsForFrontendResultSnapshot() {
        Seeker seeker = seekerWithTargetPlace(29L);
        Property property = property();
        Route route = route(seeker.getTargetPlaces().iterator().next().getTargetPlace(), property);

        when(seekerRepository.findById(1L)).thenReturn(Optional.of(seeker));
        when(aiRecommendationClient.recommend(any())).thenReturn(aiResponse());
        when(propertyRepository.findAllById(List.of(101L))).thenReturn(List.of(property));
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        InfraAccessibility pharmacy = accessibility(property, 10L, "Pharmacy", INFRA_CATEGORY.PHARMACY, 4);
        InfraAccessibility store = accessibility(property, 11L, "Store", INFRA_CATEGORY.CONVENIENT_STORE, 7);
        InfraAccessibility cafe = accessibility(property, 16L, "Cafe", INFRA_CATEGORY.CAFE, 9);
        when(infraAccessibilityRepository.findAllById(any())).thenReturn(List.of(pharmacy, store, cafe));
        when(infrastructureRepository.findAllById(List.of(10L, 11L, 16L))).thenReturn(List.of(
                pharmacy.getInfrastructure(),
                store.getInfrastructure(),
                cafe.getInfrastructure()
        ));
        when(recommendationRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Recommendation> recommendations = invocation.getArgument(0);
            ReflectionTestUtils.setField(recommendations.getFirst(), "id", 7001L);
            return recommendations;
        });

        RecommendationDtos.Data data = recommendationService.recommend(
                seeker,
                new RecommendationDtos.Request("quiet near campus", List.of("quiet"), 3)
        );

        RecommendationDtos.Result result = data.results().getFirst();
        assertThat(result.recommendationId()).isEqualTo(7001L);
        assertThat(result.property().location().latitude()).isEqualTo(37.2945);
        assertThat(result.property().tradeType()).isEqualTo(TradeType.MONTHLY_RENT);
        assertThat(result.property().depositAmount()).isEqualTo(500);
        assertThat(result.property().monthlyRent()).isEqualTo(55);
        assertThat(result.property().maintenanceFee()).isEqualTo(5);
        assertThat(result.property().description()).isEqualTo("Quiet studio near campus.");
        assertThat(result.property().tags()).containsExactly("quiet", "campus");
        assertThat(result.firstTargetPlaceRoute().targetPlaceId()).isEqualTo(29L);
        assertThat(result.firstTargetPlaceRoute().durationMinutes()).isEqualTo(18);
        assertThat(result.firstTargetPlaceRoute().subPaths()).hasSize(1);
        assertThat(result.infrastructures())
                .extracting(RecommendationDtos.InfrastructureDetails::walkingMinutes)
                .containsExactly(4, 7, 9);
        assertThat(result.infrastructures())
                .extracting(item -> item.location().latitude())
                .containsExactly(37.2940, 37.2940, 37.2940);

        ArgumentCaptor<AiRecommendationDtos.Request> requestCaptor = ArgumentCaptor.forClass(
                AiRecommendationDtos.Request.class
        );
        verify(aiRecommendationClient).recommend(requestCaptor.capture());
        assertThat(requestCaptor.getValue().seekerId()).isEqualTo(1L);
    }

    private Seeker seekerWithTargetPlace(Long targetPlaceId) {
        Seeker seeker = Seeker.builder()
                .name("Seeker")
                .email("seeker@example.test")
                .provider("google")
                .loginId("google_seeker")
                .build();
        ReflectionTestUtils.setField(seeker, "id", 1L);

        TargetPlace targetPlace = new TargetPlace(
                PLACE_CATEGORY.SCHOOL,
                "Campus",
                new RoadAddress("1 Campus-ro"),
                37.2950,
                126.9750
        );
        ReflectionTestUtils.setField(targetPlace, "id", targetPlaceId);
        seeker.addTargetPlace(targetPlace, null);
        return seeker;
    }

    private AiRecommendationDtos.Response aiResponse() {
        return new AiRecommendationDtos.Response(
                true,
                "Recommendation completed.",
                List.of(new AiRecommendationDtos.Result(
                        101L,
                        List.of(10L, 11L, 16L),
                        "Quiet studio with useful infrastructure."
                ))
        );
    }

    private Property property() {
        return Property.builder()
                .propertyId(101L)
                .latitude(37.2945)
                .longitude(126.9748)
                .tradeType(TradeType.MONTHLY_RENT)
                .deposit(500)
                .monthlyRent(55)
                .maintenanceFee(5)
                .description("Quiet studio near campus.")
                .tags(List.of("quiet", "campus"))
                .build();
    }

    private Route route(TargetPlace targetPlace, Property property) {
        return new Route(
                targetPlace,
                property,
                TRANSPORT_MODE.PUBLIC_TRANSPORT,
                new Minutes(18),
                new Path(
                        new Minutes(18),
                        1,
                        List.of(new SubPath(3, new Minutes(6), "Campus", "Property", null))
                )
        );
    }

    private InfraAccessibility accessibility(
            Property property,
            Long infrastructureId,
            String name,
            INFRA_CATEGORY category,
            int walkingMinutes
    ) {
        Infrastructure infrastructure = new Infrastructure(
                name,
                category,
                37.2940,
                126.9740,
                new RoadAddress(infrastructureId + " Infra-ro")
        );
        ReflectionTestUtils.setField(infrastructure, "id", infrastructureId);
        return new InfraAccessibility(property, infrastructure, new Minutes(walkingMinutes), null);
    }
}
