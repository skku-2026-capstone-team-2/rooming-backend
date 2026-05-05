package com.skku.zip.domain.locations.service;

import com.skku.zip.domain.locations.client.OdsayClient;
import com.skku.zip.domain.locations.client.TmapClient;
import com.skku.zip.domain.locations.dto.OdsayRouteCandidate;
import com.skku.zip.domain.locations.dto.TmapUserplaceInfo;
import com.skku.zip.domain.locations.entity.Route;
import com.skku.zip.domain.locations.entity.TRANSPORT_MODE;
import com.skku.zip.domain.locations.entity.Userplace;
import com.skku.zip.domain.locations.repository.NearbyPropertyQueryRepository;
import com.skku.zip.domain.property.entity.Property;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserplaceRouteService {

    private final TmapClient tmapClient;
    private final OdsayClient odsayClient;
    private final NearbyPropertyQueryRepository nearbyPropertyQueryRepository;

    public Userplace createUserplace(double latitude, double longitude) {
        TmapUserplaceInfo placeInfo = tmapClient.resolveUserplace(latitude, longitude);
        return new Userplace(latitude, longitude, placeInfo.name(), placeInfo.category(), placeInfo.address());
    }

    @Transactional(readOnly = true)
    public List<Route> buildRoutesToPropertiesWithinFiveKilometers(Userplace userplace) {
        if (userplace.getId() == null) {
            throw new IllegalArgumentException("Save Userplace before building Route entities.");
        }

        return nearbyPropertyQueryRepository.findWithinFiveKilometers(userplace).stream()
                .map(property -> buildRoute(userplace, property))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingInt(route -> route.getDurationMinutes().getValue()))
                .toList();
    }

    private Optional<Route> buildRoute(Userplace userplace, Property property) {
        return odsayClient.findFastestRoute(
                        userplace.getLatitude(),
                        userplace.getLongitude(),
                        property.getLatitude(),
                        property.getLongitude()
                )
                .map(candidate -> toRoute(userplace, property, candidate));
    }

    private Route toRoute(Userplace userplace, Property property, OdsayRouteCandidate candidate) {
        return new Route(
                userplace,
                property,
                TRANSPORT_MODE.PUBLIC_TRANSPORT,
                candidate.duration(),
                candidate.path()
        );
    }
}
