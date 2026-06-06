package com.rooming.domain.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rooming.common.exception.NotFoundException;
import com.rooming.domain.locations.dto.CoordinateDto;
import com.rooming.domain.locations.dto.RouteGeometryDetail;
import com.rooming.domain.locations.entity.id.PropertyInfrastructureId;
import com.rooming.domain.locations.entity.id.TargetPlacePropertyId;
import com.rooming.domain.locations.entity.model.InfraAccessibility;
import com.rooming.domain.locations.entity.model.Infrastructure;
import com.rooming.domain.locations.entity.model.Route;
import com.rooming.domain.locations.entity.model.TargetPlace;
import com.rooming.domain.locations.entity.value.Minutes;
import com.rooming.domain.locations.entity.value.Path;
import com.rooming.domain.locations.entity.value.RoutePoint;
import com.rooming.domain.locations.entity.value.SubPath;
import com.rooming.domain.locations.repository.InfraAccessibilityRepository;
import com.rooming.domain.locations.repository.InfrastructureRepository;
import com.rooming.domain.locations.repository.RouteRepository;
import com.rooming.domain.locations.service.PropertyInfrastructureService;
import com.rooming.domain.locations.service.RouteGeometryService;
import com.rooming.domain.property.entity.Property;
import com.rooming.domain.property.repository.PropertyRepository;
import com.rooming.domain.recommendation.client.AiRecommendationClient;
import com.rooming.domain.recommendation.dto.AiRecommendationDtos;
import com.rooming.domain.recommendation.dto.RecommendationDtos;
import com.rooming.domain.recommendation.entity.Recommendation;
import com.rooming.domain.recommendation.repository.RecommendationRepository;
import com.rooming.domain.seeker.entity.RegisteredTargetPlace;
import com.rooming.domain.seeker.entity.Seeker;
import com.rooming.domain.seeker.repository.SeekerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final AiRecommendationClient aiRecommendationClient;
    private final SeekerRepository seekerRepository;
    private final PropertyRepository propertyRepository;
    private final RouteRepository routeRepository;
    private final RouteGeometryService routeGeometryService;
    private final InfraAccessibilityRepository infraAccessibilityRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final PropertyInfrastructureService propertyInfrastructureService;
    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public RecommendationDtos.Data recommend(Seeker seeker, RecommendationDtos.Request request) {
        Seeker managedSeeker = findSeeker(seeker);
        List<RegisteredTargetPlace> targetPlaces = targetPlaces(managedSeeker);
        AiRecommendationDtos.Response aiResponse = aiRecommendationClient.recommend(
                toAiRequest(request, managedSeeker)
        );

        List<RecommendationDtos.Result> savedResults = List.of();
        if (Boolean.TRUE.equals(aiResponse.success())) {
            List<AiRecommendationDtos.Result> aiResults = aiResults(aiResponse);
            Map<Long, Property> propertiesById = propertiesById(aiResults);
            Long firstTargetPlaceId = firstTargetPlaceId(targetPlaces);
            List<RecommendationDtos.Result> snapshots = aiResults.stream()
                    .map(result -> toResult(result, propertiesById, firstTargetPlaceId))
                    .filter(Objects::nonNull)
                    .toList();

            savedResults = saveRecommendations(managedSeeker, snapshots, propertiesById).stream()
                    .map(this::toResult)
                    .toList();
        }

        return new RecommendationDtos.Data(aiMessage(aiResponse), savedResults);
    }

    @Transactional(readOnly = true)
    public RecommendationDtos.ListData getRecommendations(Seeker seeker) {
        Seeker managedSeeker = findSeeker(seeker);
        return new RecommendationDtos.ListData(
                recommendationRepository.findAllBySeekerOrderByCreatedAtDescIdDesc(managedSeeker).stream()
                        .map(this::toResult)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public RecommendationDtos.FavoriteData getFavoriteRecommendations(Seeker seeker) {
        Seeker managedSeeker = findSeeker(seeker);
        return new RecommendationDtos.FavoriteData(
                recommendationRepository
                        .findAllBySeekerAndFavoriteTrueOrderByFavoritedAtDescCreatedAtDescIdDesc(managedSeeker).stream()
                        .map(this::toResult)
                        .toList()
        );
    }

    @Transactional
    public RecommendationDtos.Result favoriteRecommendation(Seeker seeker, Long recommendationId) {
        Recommendation recommendation = findRecommendation(findSeeker(seeker), recommendationId);
        recommendation.markFavorite();
        return toResult(recommendation);
    }

    @Transactional(readOnly = true)
    public RecommendationDtos.RouteDetailData getRecommendationRoute(
            Seeker seeker,
            Long recommendationId,
            RouteGeometryDetail detail
    ) {
        Recommendation recommendation = findRecommendation(findSeeker(seeker), recommendationId);
        Route route = findRoute(recommendation, snapshot(recommendation));
        Path projectedPath = routeGeometryService.project(route.getRouteJson(), detail);
        return toRouteDetailData(recommendation, route, detail, projectedPath);
    }

    @Transactional
    public void removeFavoriteRecommendation(Seeker seeker, Long recommendationId) {
        Recommendation recommendation = findRecommendation(findSeeker(seeker), recommendationId);
        recommendation.removeFavorite();
    }

    @Transactional
    public void deleteRecommendation(Seeker seeker, Long recommendationId) {
        Recommendation recommendation = findRecommendation(findSeeker(seeker), recommendationId);
        recommendationRepository.delete(recommendation);
    }

    private List<Recommendation> saveRecommendations(
            Seeker seeker,
            List<RecommendationDtos.Result> snapshots,
            Map<Long, Property> propertiesById
    ) {
        return recommendationRepository.saveAll(snapshots.stream()
                .map(snapshot -> new Recommendation(
                        seeker,
                        propertiesById.get(snapshot.propertyId()),
                        objectMapper.valueToTree(snapshot)
                ))
                .toList());
    }

    private Seeker findSeeker(Seeker seeker) {
        if (seeker == null || seeker.getId() == null) {
            throw new NotFoundException("Seeker not found.");
        }
        return seekerRepository.findById(seeker.getId())
                .orElseThrow(() -> new NotFoundException("Seeker not found."));
    }

    private Recommendation  findRecommendation(Seeker seeker, Long recommendationId) {
        return recommendationRepository.findByIdAndSeeker(recommendationId, seeker)
                .orElseThrow(() -> new NotFoundException("Recommendation not found."));
    }

    private List<RegisteredTargetPlace> targetPlaces(Seeker seeker) {
        return seeker.getTargetPlaces().stream()
                .sorted(Comparator.comparing(targetPlace -> targetPlace.getTargetPlace().getId()))
                .toList();
    }

    private AiRecommendationDtos.Request toAiRequest(
            RecommendationDtos.Request request,
            Seeker seeker
    ) {
        return new AiRecommendationDtos.Request(
                request.normalizedQuery(),
                request.normalizedPreferences(),
                seeker.getId(),
                request.normalizedTopN()
        );
    }

    private List<AiRecommendationDtos.Result> aiResults(AiRecommendationDtos.Response response) {
        if (response.results() == null) {
            return List.of();
        }
        return response.results().stream()
                .filter(result -> result != null && result.propertyId() != null)
                .toList();
    }

    private Map<Long, Property> propertiesById(List<AiRecommendationDtos.Result> aiResults) {
        List<Long> propertyIds = aiResults.stream()
                .map(AiRecommendationDtos.Result::propertyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();

        if (propertyIds.isEmpty()) {
            return Map.of();
        }

        return propertyRepository.findAllById(propertyIds).stream()
                .collect(Collectors.toMap(Property::getPropertyId, Function.identity()));
    }

    private RecommendationDtos.Result toResult(
            AiRecommendationDtos.Result result,
            Map<Long, Property> propertiesById,
            Long firstTargetPlaceId
    ) {
        Property property = propertiesById.get(result.propertyId());
        if (property == null) {
            return null;
        }
        List<Long> infraIds = normalizedInfraIds(result.infraIds());
        return new RecommendationDtos.Result(
                null,
                result.propertyId(),
                false,
                propertyDetails(property),
                firstTargetPlaceRoute(property, firstTargetPlaceId),
                infrastructures(property, infraIds),
                result.explanation()
        );
    }

    private RecommendationDtos.Result toResult(Recommendation recommendation) {
        RecommendationDtos.Result snapshot = snapshot(recommendation);
        List<Long> infrastructureIds = infrastructureIds(recommendation, snapshot);
        Property property = recommendation.getProperty();
        RecommendationDtos.PropertyDetails propertyDetails = snapshot.property() == null
                ? propertyDetails(property)
                : snapshot.property();
        RecommendationDtos.TargetPlaceRoute firstTargetPlaceRoute =
                firstTargetPlaceRoute(recommendation, snapshot);
        List<RecommendationDtos.InfrastructureDetails> infrastructures = normalizedInfrastructures(
                snapshot.infrastructures()
        );
        if (infrastructures.isEmpty() && !infrastructureIds.isEmpty()) {
            infrastructures = infrastructures(property, infrastructureIds);
        }
        return new RecommendationDtos.Result(
                recommendation.getId(),
                snapshot.propertyId(),
                recommendation.isFavorite(),
                propertyDetails,
                firstTargetPlaceRoute,
                infrastructures,
                snapshot.explanation()
        );
    }

    private List<Long> normalizedInfraIds(List<Long> infraIds) {
        if (infraIds == null) {
            return List.of();
        }
        return infraIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private Long firstTargetPlaceId(List<RegisteredTargetPlace> targetPlaces) {
        return targetPlaces.stream()
                .map(RegisteredTargetPlace::getTargetPlace)
                .map(TargetPlace::getId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private RecommendationDtos.PropertyDetails propertyDetails(Property property) {
        return new RecommendationDtos.PropertyDetails(
                coordinate(property),
                property.getTradeType(),
                property.getDeposit(),
                property.getMonthlyRent(),
                property.getMaintenanceFee(),
                property.getDescription(),
                property.getTags() == null ? List.of() : property.getTags()
        );
    }

    private CoordinateDto coordinate(Property property) {
        if (property.getLatitude() == null || property.getLongitude() == null) {
            return null;
        }
        return new CoordinateDto(property.getLatitude(), property.getLongitude());
    }

    private RecommendationDtos.TargetPlaceRoute firstTargetPlaceRoute(Property property, Long targetPlaceId) {
        if (targetPlaceId == null) {
            return null;
        }
        return routeRepository.findById(new TargetPlacePropertyId(targetPlaceId, property.getPropertyId()))
                .map(this::toTargetPlaceRoute)
                .orElse(null);
    }

    private RecommendationDtos.TargetPlaceRoute toTargetPlaceRoute(Route route) {
        Path path = route.getRouteJson();
        return new RecommendationDtos.TargetPlaceRoute(
                route.getId().getTargetPlaceId(),
                route.getTransportMode().name(),
                minutesValue(route.getDurationMinutes()),
                path == null ? 0 : path.getTransferCount(),
                routeSubPathSummaries(path)
        );
    }

    private RecommendationDtos.TargetPlaceRoute firstTargetPlaceRoute(
            Recommendation recommendation,
            RecommendationDtos.Result snapshot
    ) {
        Long targetPlaceId = routeTargetPlaceId(recommendation, snapshot);
        RecommendationDtos.TargetPlaceRoute currentRoute = firstTargetPlaceRoute(
                recommendation.getProperty(),
                targetPlaceId
        );
        return currentRoute == null ? snapshot.firstTargetPlaceRoute() : currentRoute;
    }

    private List<RecommendationDtos.InfrastructureDetails> infrastructures(Property property, List<Long> infraIds) {
        if (infraIds.isEmpty()) {
            return List.of();
        }

        Map<Long, InfraAccessibility> accessibilitiesByInfraId = infraAccessibilityRepository.findAllById(
                        infraIds.stream()
                                .map(infraId -> new PropertyInfrastructureId(property.getPropertyId(), infraId))
                                .toList()
                ).stream()
                .collect(Collectors.toMap(
                        accessibility -> accessibility.getId().getInfrastructureId(),
                        Function.identity()
                ));
        Map<Long, Infrastructure> infrastructuresById = infrastructureRepository.findAllById(infraIds).stream()
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));

        return infraIds.stream()
                .map(infraId -> toInfrastructureDetails(
                        property,
                        infraId,
                        accessibilitiesByInfraId.get(infraId),
                        infrastructuresById.get(infraId)
                ))
                .toList();
    }

    private RecommendationDtos.InfrastructureDetails toInfrastructureDetails(
            Property property,
            Long infrastructureId,
            InfraAccessibility accessibility,
            Infrastructure infrastructure
    ) {
        InfraAccessibility effectiveAccessibility = accessibility;
        if ((effectiveAccessibility == null || !effectiveAccessibility.hasWalkingRouteJson())
                && property != null
                && infrastructure != null) {
            effectiveAccessibility = propertyInfrastructureService
                    .buildTransientInfraAccessibility(property, infrastructure)
                    .orElse(effectiveAccessibility);
            if (effectiveAccessibility != null && effectiveAccessibility.hasWalkingRouteJson()) {
                propertyInfrastructureService.storeInfraAccessibilityAsync(
                        property.getPropertyId(),
                        infrastructure.getId(),
                        effectiveAccessibility.getWalkingTime(),
                        effectiveAccessibility.getWalkingRouteJson()
                );
            }
        }

        Infrastructure selectedInfrastructure = accessibility == null
                ? infrastructure
                : accessibility.getInfrastructure();
        if (effectiveAccessibility != null) {
            selectedInfrastructure = effectiveAccessibility.getInfrastructure();
        }
        return new RecommendationDtos.InfrastructureDetails(
                infrastructureId,
                selectedInfrastructure == null ? null : selectedInfrastructure.getName(),
                category(selectedInfrastructure),
                roadAddress(selectedInfrastructure),
                coordinate(selectedInfrastructure),
                effectiveAccessibility == null ? null : minutesValue(effectiveAccessibility.getWalkingTime())
        );
    }

    private List<RecommendationDtos.InfrastructureDetails> normalizedInfrastructures(
            List<RecommendationDtos.InfrastructureDetails> infrastructures
    ) {
        return infrastructures == null ? List.of() : infrastructures;
    }

    private List<Long> infrastructureIds(Recommendation recommendation, RecommendationDtos.Result snapshot) {
        List<Long> snapshotInfrastructureIds = normalizedInfrastructures(snapshot.infrastructures()).stream()
                .map(RecommendationDtos.InfrastructureDetails::infrastructureId)
                .filter(Objects::nonNull)
                .toList();
        if (!snapshotInfrastructureIds.isEmpty()) {
            return snapshotInfrastructureIds;
        }

        if (!recommendation.getSnapshotJson().path("infraIds").isArray()) {
            return List.of();
        }
        List<Long> legacyInfraIds = new java.util.ArrayList<>();
        recommendation.getSnapshotJson().path("infraIds").forEach(node -> {
            if (node.canConvertToLong()) {
                legacyInfraIds.add(node.longValue());
            }
        });
        return normalizedInfraIds(legacyInfraIds);
    }

    private CoordinateDto coordinate(Infrastructure infrastructure) {
        if (infrastructure == null) {
            return null;
        }
        return new CoordinateDto(infrastructure.getLatitude(), infrastructure.getLongitude());
    }

    private String category(Infrastructure infrastructure) {
        return infrastructure == null || infrastructure.getCategory() == null
                ? null
                : infrastructure.getCategory().name();
    }

    private String roadAddress(Infrastructure infrastructure) {
        return infrastructure == null || infrastructure.getAddress() == null
                ? null
                : infrastructure.getAddress().getValue();
    }

    private List<RecommendationDtos.RouteSubPathSummary> routeSubPathSummaries(Path path) {
        if (path == null || path.getSubPaths() == null) {
            return List.of();
        }
        return path.getSubPaths().stream()
                .map(this::routeSubPathSummary)
                .toList();
    }

    private RecommendationDtos.RouteSubPathSummary routeSubPathSummary(SubPath subPath) {
        return new RecommendationDtos.RouteSubPathSummary(
                trafficType(subPath.getTrafficType()),
                subPath.getTrafficType(),
                minutesValue(subPath.getSectionTime()),
                subPath.getStartName(),
                subPath.getEndName(),
                subPath.getLaneName(),
                subPath.getDistanceMeters(),
                subPath.getDescription()
        );
    }

    private RecommendationDtos.RouteDetailData toRouteDetailData(
            Recommendation recommendation,
            Route route,
            RouteGeometryDetail detail,
            Path path
    ) {
        return new RecommendationDtos.RouteDetailData(
                recommendation.getId(),
                route.getId().getPropertyId(),
                route.getId().getTargetPlaceId(),
                route.getTransportMode().name(),
                minutesValue(route.getDurationMinutes()),
                detail,
                routePath(path)
        );
    }

    private RecommendationDtos.RoutePath routePath(Path path) {
        if (path == null) {
            return new RecommendationDtos.RoutePath(0, 0, 0, List.of());
        }
        return new RecommendationDtos.RoutePath(
                minutesValue(path.getTotalTime()),
                path.getTransferCount(),
                routeGeometryService.pointCount(path),
                path.getSubPaths() == null
                        ? List.of()
                        : path.getSubPaths().stream()
                        .map(this::routeSubPathDetail)
                        .toList()
        );
    }

    private RecommendationDtos.RouteSubPathDetail routeSubPathDetail(SubPath subPath) {
        return new RecommendationDtos.RouteSubPathDetail(
                trafficType(subPath.getTrafficType()),
                subPath.getTrafficType(),
                minutesValue(subPath.getSectionTime()),
                subPath.getStartName(),
                subPath.getEndName(),
                subPath.getLaneName(),
                subPath.getDistanceMeters(),
                subPath.getDescription(),
                subPath.getPoints() == null
                        ? List.of()
                        : subPath.getPoints().stream()
                        .map(this::coordinate)
                        .toList()
        );
    }

    private CoordinateDto coordinate(RoutePoint routePoint) {
        return new CoordinateDto(routePoint.getLatitude(), routePoint.getLongitude());
    }

    private Route findRoute(Recommendation recommendation, RecommendationDtos.Result snapshot) {
        Long targetPlaceId = routeTargetPlaceId(recommendation, snapshot);
        if (targetPlaceId == null) {
            throw new NotFoundException("Recommendation route not found.");
        }
        return routeRepository.findById(new TargetPlacePropertyId(
                        targetPlaceId,
                        recommendation.getProperty().getPropertyId()
                ))
                .orElseThrow(() -> new NotFoundException("Recommendation route not found."));
    }

    private Long routeTargetPlaceId(Recommendation recommendation, RecommendationDtos.Result snapshot) {
        if (snapshot.firstTargetPlaceRoute() != null && snapshot.firstTargetPlaceRoute().targetPlaceId() != null) {
            return snapshot.firstTargetPlaceRoute().targetPlaceId();
        }
        return firstTargetPlaceId(targetPlaces(recommendation.getSeeker()));
    }

    private RecommendationDtos.Result snapshot(Recommendation recommendation) {
        return objectMapper.convertValue(recommendation.getSnapshotJson(), RecommendationDtos.Result.class);
    }

    private int minutesValue(Minutes minutes) {
        return minutes == null || minutes.getValue() == null ? 0 : minutes.getValue();
    }

    private String trafficType(int trafficType) {
        return switch (trafficType) {
            case 1 -> "SUBWAY";
            case 2 -> "BUS";
            case 3 -> "WALK";
            default -> "UNKNOWN";
        };
    }

    private String aiMessage(AiRecommendationDtos.Response response) {
        if (response.message() == null || response.message().isBlank()) {
            return "Recommendation completed.";
        }
        return response.message();
    }
}
