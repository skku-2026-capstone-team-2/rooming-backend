package com.skku.zip.domain.locations.service;

import com.skku.zip.common.exception.BadRequestException;
import com.skku.zip.common.exception.NotFoundException;
import com.skku.zip.domain.locations.client.TmapClient;
import com.skku.zip.domain.locations.dto.CoordinateDto;
import com.skku.zip.domain.locations.dto.PlaceSearchItem;
import com.skku.zip.domain.locations.dto.TmapPlaceCandidate;
import com.skku.zip.domain.locations.dto.UserPlaceCreateRequest;
import com.skku.zip.domain.locations.dto.UserPlaceResponseItem;
import com.skku.zip.domain.locations.dto.UserPlaceUpdateRequest;
import com.skku.zip.domain.locations.entity.value.RoadAddress;
import com.skku.zip.domain.locations.entity.type.USER_PLACE_TYPE;
import com.skku.zip.domain.locations.entity.model.Userplace;
import com.skku.zip.domain.locations.repository.UserplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPlaceApiService {

    private final UserplaceRepository userplaceRepository;
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
    public List<UserPlaceResponseItem> getUserPlaces() {
        return userplaceRepository.findAllByOrderByIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserPlaceResponseItem createUserPlace(UserPlaceCreateRequest request) {
        Userplace userplace = new Userplace(
                request.placeType(),
                request.placeName(),
                new RoadAddress(request.roadAddress()),
                request.location().latitude(),
                request.location().longitude(),
                request.memo(),
                request.isActive() == null
        );

        return toResponse(userplaceRepository.save(userplace));
    }

    @Transactional
    public UserPlaceResponseItem updateUserPlace(Long userPlaceId, UserPlaceUpdateRequest request) {
        Userplace userplace = findUserPlace(userPlaceId);

        USER_PLACE_TYPE placeType = request.placeType() == null
                ? userplace.getPlaceType()
                : request.placeType();
        String placeName = request.placeName() == null
                ? userplace.getName()
                : request.placeName();
        RoadAddress roadAddress = request.roadAddress() == null
                ? userplace.getAddress()
                : new RoadAddress(request.roadAddress());
        CoordinateDto location = request.location() == null
                ? new CoordinateDto(userplace.getLatitude(), userplace.getLongitude())
                : request.location();
        String memo = request.memo() == null
                ? userplace.getMemo()
                : request.memo();
        boolean active = request.isActive() == null
                ? userplace.isActive()
                : request.isActive();

        userplace.updateUserPlace(
                placeType,
                placeName,
                roadAddress,
                location.latitude(),
                location.longitude(),
                memo,
                active
        );

        return toResponse(userplace);
    }

    @Transactional
    public void deleteUserPlace(Long userPlaceId) {
        Userplace userplace = findUserPlace(userPlaceId);
        userplaceRepository.delete(userplace);
    }

    private Userplace findUserPlace(Long userPlaceId) {
        return userplaceRepository.findById(userPlaceId)
                .orElseThrow(() -> new NotFoundException("User place not found."));
    }

    private PlaceSearchItem toPlaceSearchItem(TmapPlaceCandidate candidate) {
        return new PlaceSearchItem(
                candidate.placeName(),
                candidate.roadAddress(),
                new CoordinateDto(candidate.latitude(), candidate.longitude())
        );
    }

    private UserPlaceResponseItem toResponse(Userplace userplace) {
        return new UserPlaceResponseItem(
                userplace.getId(),
                userplace.getPlaceType().name(),
                userplace.getName(),
                userplace.getAddress() == null ? null : userplace.getAddress().getValue(),
                new CoordinateDto(userplace.getLatitude(), userplace.getLongitude()),
                userplace.getMemo(),
                userplace.isActive()
        );
    }
}
