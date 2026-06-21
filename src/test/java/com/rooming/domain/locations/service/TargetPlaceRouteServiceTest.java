package com.rooming.domain.locations.service;

import com.rooming.domain.locations.client.OdsayClient;
import com.rooming.domain.locations.client.TmapClient;
import com.rooming.domain.locations.dto.OdsayRouteCandidate;
import com.rooming.domain.locations.entity.id.TargetPlacePropertyId;
import com.rooming.domain.locations.entity.model.Route;
import com.rooming.domain.locations.entity.model.TargetPlace;
import com.rooming.domain.locations.entity.type.PLACE_CATEGORY;
import com.rooming.domain.locations.entity.type.TRANSPORT_MODE;
import com.rooming.domain.locations.entity.value.Minutes;
import com.rooming.domain.locations.entity.value.Path;
import com.rooming.domain.locations.entity.value.RoadAddress;
import com.rooming.domain.locations.entity.value.SubPath;
import com.rooming.domain.locations.repository.NearbyPropertyQueryRepository;
import com.rooming.domain.locations.repository.RouteRepository;
import com.rooming.domain.locations.repository.TargetPlaceRepository;
import com.rooming.domain.property.entity.Property;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class TargetPlaceRouteServiceTest {

    private final TmapClient tmapClient = mock(TmapClient.class);
    private final OdsayClient odsayClient = mock(OdsayClient.class);
    private final NearbyPropertyQueryRepository nearbyPropertyQueryRepository =
            mock(NearbyPropertyQueryRepository.class);
    private final TargetPlaceRepository targetPlaceRepository = mock(TargetPlaceRepository.class);
    private final RouteRepository routeRepository = mock(RouteRepository.class);
    private final TargetPlaceRouteService targetPlaceRouteService = new TargetPlaceRouteService(
            tmapClient,
            odsayClient,
            nearbyPropertyQueryRepository,
            targetPlaceRepository,
            routeRepository
    );

    @Test
    void closePropertyUsesTmapWalkingRouteWithoutCallingOdsay() {
        TargetPlace targetPlace = targetPlace();
        Property property = closeProperty();
        when(nearbyPropertyQueryRepository.findWithinFiveKilometers(targetPlace))
                .thenReturn(List.of(property));
        when(tmapClient.findWalkingRoute(37.2945, 126.9748, 37.2961, 126.9718))
                .thenReturn(Optional.of(routeCandidate(6)));

        List<Route> routes = targetPlaceRouteService.buildRoutesToPropertiesWithinFiveKilometers(targetPlace);

        assertThat(routes).hasSize(1);
        assertThat(routes.getFirst().getTransportMode()).isEqualTo(TRANSPORT_MODE.WALK);
        assertThat(routes.getFirst().getDurationMinutes().getValue()).isEqualTo(6);
        verify(tmapClient).findWalkingRoute(37.2945, 126.9748, 37.2961, 126.9718);
        verifyNoInteractions(odsayClient);
    }

    @Test
    void newlyStoredPropertyCreatesMissingRoutesToNearbyTargetPlaces() {
        Property property = closeProperty();
        TargetPlace walkingTarget = targetPlace();
        TargetPlace publicTransportTarget = targetPlace(
                30L,
                "Office",
                37.3300,
                126.9748
        );
        when(targetPlaceRepository.findWithinMeters(37.2961, 126.9718, 5_000.0))
                .thenReturn(List.of(walkingTarget, publicTransportTarget));
        when(routeRepository.existsById(new TargetPlacePropertyId(29L, 101L))).thenReturn(false);
        when(routeRepository.existsById(new TargetPlacePropertyId(30L, 101L))).thenReturn(false);
        when(tmapClient.findWalkingRoute(37.2945, 126.9748, 37.2961, 126.9718))
                .thenReturn(Optional.of(routeCandidate(6)));
        when(odsayClient.findFastestRoute(37.3300, 126.9748, 37.2961, 126.9718))
                .thenReturn(Optional.of(routeCandidate(18)));
        when(routeRepository.saveAllAndFlush(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Route> routes =
                targetPlaceRouteService.storeMissingRoutesToTargetPlacesWithinFiveKilometers(property);

        assertThat(routes)
                .extracting(Route::getTransportMode)
                .containsExactly(TRANSPORT_MODE.WALK, TRANSPORT_MODE.PUBLIC_TRANSPORT);
        assertThat(routes)
                .extracting(route -> route.getId().getTargetPlaceId())
                .containsExactly(29L, 30L);
        verify(targetPlaceRepository).findWithinMeters(37.2961, 126.9718, 5_000.0);
        verify(routeRepository).saveAllAndFlush(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void newlyStoredPropertySkipsRoutePairsThatAlreadyExist() {
        Property property = closeProperty();
        TargetPlace targetPlace = targetPlace();
        when(targetPlaceRepository.findWithinMeters(37.2961, 126.9718, 5_000.0))
                .thenReturn(List.of(targetPlace));
        when(routeRepository.existsById(new TargetPlacePropertyId(29L, 101L))).thenReturn(true);

        List<Route> routes =
                targetPlaceRouteService.storeMissingRoutesToTargetPlacesWithinFiveKilometers(property);

        assertThat(routes).isEmpty();
        verify(tmapClient, never()).findWalkingRoute(
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble()
        );
        verifyNoInteractions(odsayClient);
        verify(routeRepository, never()).saveAllAndFlush(org.mockito.ArgumentMatchers.anyList());
    }

    private TargetPlace targetPlace() {
        return targetPlace(29L, "Campus", 37.2945, 126.9748);
    }

    private TargetPlace targetPlace(
            Long id,
            String name,
            double latitude,
            double longitude
    ) {
        TargetPlace targetPlace = new TargetPlace(
                PLACE_CATEGORY.SCHOOL,
                name,
                new RoadAddress("2066 Seobu-ro"),
                latitude,
                longitude
        );
        ReflectionTestUtils.setField(targetPlace, "id", id);
        return targetPlace;
    }

    private Property closeProperty() {
        return Property.builder()
                .propertyId(101L)
                .latitude(37.2961)
                .longitude(126.9718)
                .build();
    }

    private OdsayRouteCandidate routeCandidate(int durationMinutes) {
        Minutes duration = new Minutes(durationMinutes);
        return new OdsayRouteCandidate(
                duration,
                new Path(
                        duration,
                        0,
                        List.of(new SubPath(3, duration, "Campus", "Property", null))
                )
        );
    }
}
