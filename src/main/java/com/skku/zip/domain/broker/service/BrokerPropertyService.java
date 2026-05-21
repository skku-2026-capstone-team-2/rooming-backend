package com.skku.zip.domain.broker.service;

import com.skku.zip.domain.broker.dto.BrokerPropertyCreateRequest;
import com.skku.zip.domain.broker.dto.BrokerPropertyData;
import com.skku.zip.domain.broker.entity.Broker;
import com.skku.zip.domain.broker.repository.BrokerPropertyRepository;
import com.skku.zip.domain.broker.repository.BrokerRepository;
import com.skku.zip.domain.locations.dto.CoordinateDto;
import com.skku.zip.domain.locations.service.PropertyInfrastructureService;
import com.skku.zip.domain.property.entity.Property;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class BrokerPropertyService {

    private static final long INITIAL_PROPERTY_ID = 101L;

    private final BrokerPropertyRepository brokerPropertyRepository;
    private final BrokerRepository brokerRepository;
    private final PropertyInfrastructureService propertyInfrastructureService;

    @Transactional
    public BrokerPropertyData createProperty(Broker broker, BrokerPropertyCreateRequest request) {
        if (broker == null || broker.getId() == null) {
            throw new IllegalArgumentException("Broker not found.");
        }
        Broker managedBroker = brokerRepository.findById(broker.getId())
                .orElseThrow(() -> new IllegalArgumentException("Broker not found."));
        if (!managedBroker.isVerified()) {
            throw new AccessDeniedException("Broker verification is required before posting a property.");
        }

        Property property = Property.builder()
                .propertyId(nextPropertyId())
                .title(request.title())
                .address(request.roadAddress())
                .latitude(request.location().latitude())
                .longitude(request.location().longitude())
                .deposit(request.depositAmount())
                .monthlyRent(request.monthlyRent())
                .maintenanceFee(request.maintenanceFee())
                .areaM2(request.areaM2().floatValue())
                .roomType(request.propertyType())
                .floorInfo(request.floorInfo())
                .description(request.description())
                .has3DModel(false)
                .imageUrls(new ArrayList<>())
                .build();

        managedBroker.addProperty(property);
        Property savedProperty = brokerPropertyRepository.save(property);
        propertyInfrastructureService.storeInfrastructureAccessibilities(savedProperty);

        return toData(savedProperty);
    }

    private Long nextPropertyId() {
        Long maxPropertyId = brokerPropertyRepository.findMaxPropertyId();
        return maxPropertyId == null ? INITIAL_PROPERTY_ID : maxPropertyId + 1;
    }

    private BrokerPropertyData toData(Property property) {
        return new BrokerPropertyData(
                property.getPropertyId(),
                property.getTitle(),
                property.getRoomType(),
                property.getAddress(),
                new CoordinateDto(property.getLatitude(), property.getLongitude()),
                property.getDeposit(),
                property.getMonthlyRent(),
                property.getMaintenanceFee(),
                property.getAreaM2() == null ? null : property.getAreaM2().doubleValue(),
                property.getFloorInfo(),
                property.getDescription(),
                property.getHas3DModel()
        );
    }
}
