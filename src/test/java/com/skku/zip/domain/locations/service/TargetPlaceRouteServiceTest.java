package com.skku.zip.domain.locations.service;

import com.skku.zip.domain.locations.client.OdsayClient;
import com.skku.zip.domain.locations.client.TmapClient;
import com.skku.zip.domain.locations.dto.OdsayRouteCandidate;
import com.skku.zip.domain.locations.entity.model.Route;
import com.skku.zip.domain.locations.entity.model.TargetPlace;
import com.skku.zip.domain.locations.entity.type.PLACE_CATEGORY;
import com.skku.zip.domain.locations.entity.type.TRANSPORT_MODE;
import com.skku.zip.domain.locations.entity.value.Minutes;
import com.skku.zip.domain.locations.entity.value.Path;
import com.skku.zip.domain.locations.entity.value.RoadAddress;
import com.skku.zip.domain.locations.entity.value.SubPath;
import com.skku.zip.domain.locations.repository.NearbyPropertyQueryRepository;
import com.skku.zip.domain.locations.repository.RouteRepository;
import com.skku.zip.domain.property.entity.Property;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TargetPlaceRouteServiceTest {

    private final TmapClient tmapClient = mock(TmapClient.class);
    private final OdsayClient odsayClient = mock(OdsayClient.class);
    private final NearbyPropertyQueryRepository nearbyPropertyQueryRepository =
            mock(NearbyPropertyQueryRepository.class);
    private final RouteRepository routeRepository = mock(RouteRepository.class);
    private final TargetPlaceRouteService targetPlaceRouteService = new TargetPlaceRouteService(
            tmapClient,
            odsayClient,
            nearbyPropertyQueryRepository,
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

    private TargetPlace targetPlace() {
        TargetPlace targetPlace = new TargetPlace(
                PLACE_CATEGORY.SCHOOL,
                "Campus",
                new RoadAddress("2066 Seobu-ro"),
                37.2945,
                126.9748
        );
        ReflectionTestUtils.setField(targetPlace, "id", 29L);
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
