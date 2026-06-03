package com.rooming.domain.property.service;

import com.rooming.common.exception.BadRequestException;
import com.rooming.common.exception.ForbiddenException;
import com.rooming.domain.broker.entity.Broker;
import com.rooming.domain.property.dto.Property3DResponse;
import com.rooming.domain.property.dto.PropertyDetailResponse;
import com.rooming.domain.property.dto.PropertyImageResponse;
import com.rooming.domain.property.entity.Property;
import com.rooming.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageStorageService propertyImageStorageService;

    @Transactional(readOnly = true)
    public Property getRawById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found: " + id));
    }

    @Transactional(readOnly = true)
    public PropertyDetailResponse getById(Long id) {
        Property p = getRawById(id);
        return PropertyDetailResponse.builder()
                .propertyId(p.getPropertyId())
                .title(p.getTitle())
                .address(p.getAddress())
                .tradeType(p.getTradeType())
                .deposit(p.getDeposit())
                .monthlyRent(p.getMonthlyRent())
                .areaM2(p.getAreaM2())
                .roomType(p.getRoomType())
                .floorInfo(p.getFloorInfo())
                .maintenanceFee(p.getMaintenanceFee())
                .description(p.getDescription())
                .tags(p.getTags())
                .has3DModel(p.getHas3DModel())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PropertyImageResponse> getImages(Long id) {
        Property p = getRawById(id);
        return toImageData(p);
    }

    @Transactional
    public List<PropertyImageResponse> updateImages(Long id, Broker broker, List<MultipartFile> images) {
        Property p = getOwnedProperty(id, broker);
        List<String> imageUrls = p.getImageUrls() == null
                ? new ArrayList<>()
                : new ArrayList<>(p.getImageUrls());
        validateImageFiles(images);
        imageUrls.addAll(propertyImageStorageService.storePropertyImages(p.getPropertyId(), images));
        p.setImageUrls(imageUrls);

        return toImageData(p);
    }

    @Transactional
    public List<PropertyImageResponse> deleteImage(Long id, Broker broker, Integer imageId) {
        Property p = getOwnedProperty(id, broker);
        List<String> imageUrls = p.getImageUrls() == null
                ? new ArrayList<>()
                : new ArrayList<>(p.getImageUrls());
        if (imageId == null || imageId < 1 || imageId > imageUrls.size()) {
            throw new IllegalArgumentException("Property image not found.");
        }

        String imageUrl = imageUrls.get(imageId - 1);
        propertyImageStorageService.deletePropertyImage(imageUrl);
        imageUrls.remove(imageId - 1);
        p.setImageUrls(imageUrls);

        return toImageData(p);
    }

    private Property getOwnedProperty(Long id, Broker broker) {
        Property property = getRawById(id);
        Long brokerId = broker == null ? null : broker.getId();
        Long ownerBrokerId = property.getBroker() == null ? null : property.getBroker().getId();
        if (brokerId == null || ownerBrokerId == null || !ownerBrokerId.equals(brokerId)) {
            throw new ForbiddenException("Only the broker who registered this property can update property images.");
        }
        return property;
    }

    private void validateImageFiles(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new BadRequestException("At least one image file is required.");
        }
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                throw new BadRequestException("Image file must not be empty.");
            }
            String contentType = image.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                throw new BadRequestException("Only image files are allowed.");
            }
        }
    }

    private List<PropertyImageResponse> toImageData(Property property) {
        List<String> urls = property.getImageUrls() != null ? property.getImageUrls() : List.of();
        return IntStream.range(0, urls.size())
                .mapToObj(i -> PropertyImageResponse.builder()
                        .imageId(i + 1)
                        .imageUrl(urls.get(i))
                        .imageOrder(i + 1)
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public Property3DResponse get3D(Long id) {
        Property p = getRawById(id);
        return Property3DResponse.builder()
                .propertyId(p.getPropertyId())
                .has3DModel(p.getHas3DModel())
                .modelType("spline")
                .modelUrl(p.getSplineUrl())
                .previewImageUrl(null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Property> getAll() {
        return propertyRepository.findAll();
    }
}
