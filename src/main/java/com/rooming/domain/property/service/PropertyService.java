package com.rooming.domain.property.service;

import com.rooming.domain.property.dto.Property3DResponse;
import com.rooming.domain.property.dto.PropertyDetailResponse;
import com.rooming.domain.property.dto.PropertyImageResponse;
import com.rooming.domain.property.entity.Property;
import com.rooming.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;

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
        List<String> urls = p.getImageUrls() != null ? p.getImageUrls() : List.of();
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