package com.skku.zip.domain.locations.service;

import com.skku.zip.domain.locations.client.OdsayClient;
import com.skku.zip.domain.locations.client.TmapClient;
import com.skku.zip.domain.locations.dto.OdsayRouteCandidate;
import com.skku.zip.domain.locations.entity.model.Route;
import com.skku.zip.domain.locations.entity.model.TargetPlace;
import com.skku.zip.domain.locations.entity.id.TargetPlacePropertyId;
import com.skku.zip.domain.locations.entity.type.TRANSPORT_MODE;
import com.skku.zip.domain.locations.repository.NearbyPropertyQueryRepository;
import com.skku.zip.domain.locations.repository.RouteRepository;
import com.skku.zip.domain.property.entity.Property;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TargetPlaceRouteService {

    private static final double CLOSE_WALKING_ROUTE_THRESHOLD_KM = 0.7;
    private static final double EARTH_RADIUS_KM = 6_371.0;

    private final TmapClient tmapClient;
    private final OdsayClient odsayClient;
    private final NearbyPropertyQueryRepository nearbyPropertyQueryRepository;
    private final RouteRepository routeRepository;

    @Transactional(readOnly = true)
    public List<Route> buildRoutesToPropertiesWithinFiveKilometers(TargetPlace targetPlace) {
        if (targetPlace.getId() == null) {
            throw new IllegalArgumentException("Save TargetPlace before building Route entities.");
        }

        return nearbyPropertyQueryRepository.findWithinFiveKilometers(targetPlace).stream()
                .map(property -> buildRoute(targetPlace, property))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingInt(route -> route.getDurationMinutes().getValue()))
                .toList();
    }

    @Transactional
    public List<Route> storeMissingRoutesToPropertiesWithinFiveKilometers(TargetPlace targetPlace) {
        if (targetPlace.getId() == null) {
            throw new IllegalArgumentException("Save TargetPlace before storing Route entities.");
        }

        List<Route> routes = nearbyPropertyQueryRepository.findWithinFiveKilometers(targetPlace).stream()
                .filter(property -> property.getPropertyId() != null)
                .filter(property -> !routeRepository.existsById(
                        new TargetPlacePropertyId(targetPlace.getId(), property.getPropertyId())
                ))
                .map(property -> buildRoute(targetPlace, property))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingInt(route -> route.getDurationMinutes().getValue()))
                .toList();

        return routeRepository.saveAll(routes);
    }

    private Optional<Route> buildRoute(TargetPlace targetPlace, Property property) {
        double startLatitude = targetPlace.getLatitude();
        double startLongitude = targetPlace.getLongitude();
        double endLatitude = property.getLatitude();
        double endLongitude = property.getLongitude();

        return odsayClient.findFastestRoute(startLatitude, startLongitude, endLatitude, endLongitude)
                .map(candidate -> toRoute(targetPlace, property, TRANSPORT_MODE.PUBLIC_TRANSPORT, candidate))
                .or(() -> isCloseWalkingDistance(startLatitude, startLongitude, endLatitude, endLongitude)
                        ? tmapClient.findWalkingRoute(startLatitude, startLongitude, endLatitude, endLongitude)
                                .map(candidate -> toRoute(targetPlace, property, TRANSPORT_MODE.WALK, candidate))
                        : Optional.empty());
    }

    private boolean isCloseWalkingDistance(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        double startLatRadians = Math.toRadians(startLatitude);
        double endLatRadians = Math.toRadians(endLatitude);
        double deltaLatRadians = Math.toRadians(endLatitude - startLatitude);
        double deltaLonRadians = Math.toRadians(endLongitude - startLongitude);

        double haversine = Math.sin(deltaLatRadians / 2) * Math.sin(deltaLatRadians / 2)
                + Math.cos(startLatRadians) * Math.cos(endLatRadians)
                * Math.sin(deltaLonRadians / 2) * Math.sin(deltaLonRadians / 2);
        double distanceKm = EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return distanceKm <= CLOSE_WALKING_ROUTE_THRESHOLD_KM;
    }

    private Route toRoute(
            TargetPlace targetPlace,
            Property property,
            TRANSPORT_MODE transportMode,
            OdsayRouteCandidate candidate
    ) {
        return new Route(
                targetPlace,
                property,
                transportMode,
                candidate.duration(),
                candidate.path()
        );
    }
}
