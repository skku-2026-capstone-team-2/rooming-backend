package com.skku.zip.domain.locations.service;

import com.skku.zip.common.exception.BadRequestException;
import com.skku.zip.common.exception.NotFoundException;
import com.skku.zip.domain.locations.client.TmapClient;
import com.skku.zip.domain.locations.dto.CoordinateDto;
import com.skku.zip.domain.locations.dto.PlaceSearchItem;
import com.skku.zip.domain.locations.dto.TmapPlaceCandidate;
import com.skku.zip.domain.locations.dto.TargetPlaceCreateRequest;
import com.skku.zip.domain.locations.dto.TargetPlaceResponseItem;
import com.skku.zip.domain.locations.dto.TargetPlaceUpdateRequest;
import com.skku.zip.domain.locations.entity.model.TargetPlace;
import com.skku.zip.domain.locations.entity.type.PLACE_CATEGORY;
import com.skku.zip.domain.locations.entity.value.RoadAddress;
import com.skku.zip.domain.locations.repository.TargetPlaceRepository;
import com.skku.zip.domain.seeker.entity.Seeker;
import com.skku.zip.domain.seeker.entity.RegisteredTargetPlace;
import com.skku.zip.domain.seeker.repository.SeekerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TargetPlaceApiService {

    private final TargetPlaceRepository targetPlaceRepository;
    private final SeekerRepository seekerRepository;
    private final TargetPlaceRouteService targetPlaceRouteService;
    private final TmapClient tmapClient;

    @Transactional(readOnly = true)
    public List<PlaceSearchItem> searchPlaces(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BadRequestException("keyword must not be blank.");
        }

        return tmapClient.searchPlaces(keyword).stream()
                .map(this::toPlaceSearchItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TargetPlaceResponseItem> getTargetPlaces(Seeker seeker) {
        return findSeeker(seeker).getTargetPlaces().stream()
                .sorted(Comparator.comparing(targetPlace -> targetPlace.getTargetPlace().getId()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TargetPlaceResponseItem createTargetPlace(Seeker seeker, TargetPlaceCreateRequest request) {
        Seeker managedSeeker = findSeeker(seeker);
        TargetPlace targetPlace = findOrCreateTargetPlace(
                request.category(),
                request.placeName(),
                request.roadAddress(),
                request.location()
        );

        RegisteredTargetPlace registeredTargetPlace = managedSeeker.addTargetPlace(
                targetPlace,
                request.memo()
        );
        targetPlaceRouteService.storeMissingRoutesToPropertiesWithinFiveKilometers(targetPlace);

        return toResponse(registeredTargetPlace);
    }

    @Transactional
    public TargetPlaceResponseItem updateTargetPlace(Seeker seeker, Long targetPlaceId, TargetPlaceUpdateRequest request) {
        Seeker managedSeeker = findSeeker(seeker);
        RegisteredTargetPlace currentTargetPlace = requireCurrentTargetPlace(managedSeeker, targetPlaceId);
        TargetPlace currentPlace = currentTargetPlace.getTargetPlace();

        PLACE_CATEGORY category = request.category() == null
                ? currentPlace.getCategory()
                : request.category();
        String placeName = request.placeName() == null
                ? currentPlace.getName()
                : request.placeName();
        String roadAddress = request.roadAddress() == null
                ? addressValue(currentPlace)
                : request.roadAddress();
        CoordinateDto location = request.location() == null
                ? new CoordinateDto(currentPlace.getLatitude(), currentPlace.getLongitude())
                : request.location();
        String memo = request.memo() == null
                ? currentTargetPlace.getMemo()
                : request.memo();

        TargetPlace targetPlace = findOrCreateTargetPlace(
                category,
                placeName,
                roadAddress,
                location
        );

        RegisteredTargetPlace registeredTargetPlace = managedSeeker.replaceTargetPlace(
                currentTargetPlace,
                targetPlace,
                memo
        );
        targetPlaceRouteService.storeMissingRoutesToPropertiesWithinFiveKilometers(targetPlace);

        return toResponse(registeredTargetPlace);
    }

    @Transactional
    public void deleteTargetPlace(Seeker seeker, Long targetPlaceId) {
        Seeker managedSeeker = findSeeker(seeker);
        RegisteredTargetPlace targetPlace = requireTargetPlaceAssociation(managedSeeker, targetPlaceId);
        managedSeeker.removeTargetPlace(targetPlace);
    }

    private Seeker findSeeker(Seeker seeker) {
        if (seeker == null || seeker.getId() == null) {
            throw new NotFoundException("Seeker not found.");
        }
        return seekerRepository.findById(seeker.getId())
                .orElseThrow(() -> new NotFoundException("Seeker not found."));
    }

    private RegisteredTargetPlace requireCurrentTargetPlace(Seeker seeker, Long targetPlaceId) {
        return requireTargetPlaceAssociation(seeker, targetPlaceId);
    }

    private RegisteredTargetPlace requireTargetPlaceAssociation(Seeker seeker, Long targetPlaceId) {
        return seeker.getTargetPlaces().stream()
                .filter(targetPlace -> targetPlace.hasTargetPlaceId(targetPlaceId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Target place not found."));
    }

    private String addressValue(TargetPlace targetPlace) {
        if (targetPlace.getAddress() == null) {
            throw new BadRequestException("roadAddress must be provided.");
        }
        return targetPlace.getAddress().getValue();
    }

    private TargetPlace findOrCreateTargetPlace(
            PLACE_CATEGORY category,
            String placeName,
            String roadAddress,
            CoordinateDto location
    ) {
        RoadAddress address = new RoadAddress(roadAddress);
        String normalizedName = placeName == null ? null : placeName.trim();

        return targetPlaceRepository.findByAddress(address)
                .orElseGet(() -> targetPlaceRepository.save(new TargetPlace(
                        category,
                        normalizedName,
                        address,
                        location.latitude(),
                        location.longitude()
                )));
    }

    private PlaceSearchItem toPlaceSearchItem(TmapPlaceCandidate candidate) {
        return new PlaceSearchItem(
                candidate.placeName(),
                candidate.roadAddress(),
                new CoordinateDto(candidate.latitude(), candidate.longitude())
        );
    }

    private TargetPlaceResponseItem toResponse(RegisteredTargetPlace registeredTargetPlace) {
        TargetPlace targetPlace = registeredTargetPlace.getTargetPlace();
        return new TargetPlaceResponseItem(
                targetPlace.getId(),
                targetPlace.getCategory().name(),
                targetPlace.getName(),
                targetPlace.getAddress() == null ? null : targetPlace.getAddress().getValue(),
                new CoordinateDto(targetPlace.getLatitude(), targetPlace.getLongitude()),
                registeredTargetPlace.getMemo()
        );
    }
}
