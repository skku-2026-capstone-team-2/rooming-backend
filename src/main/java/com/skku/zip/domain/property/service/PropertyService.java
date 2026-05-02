package com.skku.zip.domain.property.service;

import com.skku.zip.domain.property.dto.AdminCreatePropertyRequest;
import com.skku.zip.domain.property.dto.AdminProperty3DRequest;
import com.skku.zip.domain.property.dto.AdminUpdatePropertyRequest;
import com.skku.zip.domain.property.dto.Property3DResponse;
import com.skku.zip.domain.property.dto.PropertyDetailResponse;
import com.skku.zip.domain.property.dto.PropertyImageResponse;
import com.skku.zip.domain.property.entity.Property;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class PropertyService {

    private final AtomicLong idSequence = new AtomicLong(104L);

    private final List<Property> dummyData = new ArrayList<>(List.of(
            Property.builder()
                    .propertyId(101L)
                    .title("성대 정문 도보 5분 원룸")
                    .address("경기도 수원시 장안구 영화동 123-4")
                    .deposit(500).monthlyRent(45)
                    .areaM2(20.5f).roomType("one_room").floorInfo("3층")
                    .has3DModel(true)
                    .splineUrl("https://prod.spline.design/ufDr-JPaF1n5x4ZL/scene.splinecode")
                    .imageUrls(List.of("//images/101_room_main.png"))
                    .build(),
            Property.builder()
                    .propertyId(102L)
                    .title("캠퍼스빌 502호")
                    .address("경기도 수원시 장안구 천천동 45-2")
                    .deposit(300).monthlyRent(48)
                    .areaM2(18.0f).roomType("one_room").floorInfo("5층")
                    .has3DModel(true)
                    .splineUrl("https://prod.spline.design/G9XY7eM4s61H1qA8/scene.splinecode")
                    .imageUrls(List.of())
                    .build(),
            Property.builder()
                    .propertyId(103L)
                    .title("성대 쪽문 신축 풀옵션 '율전스테이'")
                    .address("경기도 수원시 장안구 율전동 290-11")
                    .deposit(1000).monthlyRent(60)
                    .areaM2(22.5f).roomType("one_room_split") // 분리형 원룸
                    .floorInfo("2층")
                    .has3DModel(true)
                    .splineUrl("https://prod.spline.design/t7nMhC63hlht2v4b/scene.splinecode") // 새로운 모델 URL
                    .imageUrls(List.of("/images/103_room_main.png", "/images/103_room_bath.png"))
                    .build()
    ));

    public Property getRawById(Long id) {
        return dummyData.stream()
                .filter(p -> p.getPropertyId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Property not found: " + id));
    }


    public PropertyDetailResponse getById(Long id) {
        Property p = getRawById(id);
        return PropertyDetailResponse.builder()
                .propertyId(p.getPropertyId())
                .title(p.getTitle())
                .address(p.getAddress())
                .deposit(p.getDeposit())
                .monthlyRent(p.getMonthlyRent())
                .areaM2(p.getAreaM2())
                .roomType(p.getRoomType())
                .floorInfo(p.getFloorInfo())
                .has3DModel(p.getHas3DModel())
                .build();
    }

    public List<PropertyImageResponse> getImages(Long id) {
        Property p = getRawById(id);
        List<String> urls = p.getImageUrls() != null ? p.getImageUrls() : List.of();
        return IntStream.range(0, urls.size())
                .mapToObj(i -> PropertyImageResponse.builder()
                        .imageId(i + 1)
                        .imageUrl(urls.get(i))
                        .imageOrder(i + 1)
                        .build())
                .collect(Collectors.toList());
    }

    public Property3DResponse get3D(Long id) {
        Property p = getRawById(id);
        return Property3DResponse.builder()
                .propertyId(p.getPropertyId())
                .has3DModel(p.getHas3DModel())
                .modelType("spline")
                .modelUrl(p.getSplineUrl())
                .previewImageUrl(null) // 나중에 추가
                .build();
    }

    public List<Property> getAll() {
        return dummyData;
    }

    public void createProperty(AdminCreatePropertyRequest request) {
        Property property = Property.builder()
                .propertyId(idSequence.getAndIncrement())
                .title(request.getTitle())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .deposit(request.getDeposit())
                .monthlyRent(request.getMonthlyRent())
                .areaM2(request.getAreaM2())
                .roomType(request.getRoomType())
                .floorInfo(request.getFloorInfo())
                .maintenanceFee(request.getMaintenanceFee())
                .description(request.getDescription())
                .has3DModel(false)
                .imageUrls(new ArrayList<>())
                .build();
        dummyData.add(property);
    }

    public void updateProperty(Long propertyId, AdminUpdatePropertyRequest request) {
        Property p = getRawById(propertyId);
        if (request.getTitle() != null) p.setTitle(request.getTitle());
        if (request.getAddress() != null) p.setAddress(request.getAddress());
        if (request.getDeposit() != null) p.setDeposit(request.getDeposit());
        if (request.getMonthlyRent() != null) p.setMonthlyRent(request.getMonthlyRent());
        if (request.getAreaM2() != null) p.setAreaM2(request.getAreaM2());
        if (request.getRoomType() != null) p.setRoomType(request.getRoomType());
        if (request.getFloorInfo() != null) p.setFloorInfo(request.getFloorInfo());
        if (request.getMaintenanceFee() != null) p.setMaintenanceFee(request.getMaintenanceFee());
        if (request.getDescription() != null) p.setDescription(request.getDescription());
    }

    public void update3DAsset(Long propertyId, AdminProperty3DRequest request) {
        Property p = getRawById(propertyId);
        p.setHas3DModel(true);
        p.setSplineUrl(request.getModelUrl());
    }
}