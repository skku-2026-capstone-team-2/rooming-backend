package com.skku.zip.domain.broker.service;

import com.skku.zip.common.exception.ForbiddenException;
import com.skku.zip.common.exception.NotFoundException;
import com.skku.zip.domain.broker.dto.BrokerPropertyCreateRequest;
import com.skku.zip.domain.broker.dto.BrokerPropertyData;
import com.skku.zip.domain.broker.entity.Broker;
import com.skku.zip.domain.broker.repository.BrokerPropertyRepository;
import com.skku.zip.domain.broker.repository.BrokerRepository;
import com.skku.zip.domain.locations.dto.CoordinateDto;
import com.skku.zip.domain.locations.service.PropertyInfrastructureService;
import com.skku.zip.domain.property.entity.Property;
import com.skku.zip.domain.property.entity.TradeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BrokerPropertyService {

    private final BrokerPropertyRepository brokerPropertyRepository;
    private final BrokerRepository brokerRepository;
    private final PropertyInfrastructureService propertyInfrastructureService;

    @Transactional
    public BrokerPropertyData createProperty(Broker broker, BrokerPropertyCreateRequest request) {
        if (broker == null || broker.getId() == null) {
            throw new NotFoundException("Broker not found.");
        }
        Broker managedBroker = brokerRepository.findById(broker.getId())
                .orElseThrow(() -> new NotFoundException("Broker not found."));
        if (!managedBroker.isVerified()) {
            throw new ForbiddenException("Broker verification is required before posting a property.");
        }

        TradeType tradeType = request.tradeType();
        Property property = Property.builder()
                .title(request.title())
                .address(request.roadAddress())
                .latitude(request.location().latitude())
                .longitude(request.location().longitude())
                .tradeType(tradeType)
                .deposit(request.depositAmount())
                .monthlyRent(tradeType == TradeType.DEPOSIT_BASIS ? 0 : request.monthlyRent())
                .maintenanceFee(request.maintenanceFee())
                .areaM2(request.areaM2().floatValue())
                .roomType(request.propertyType())
                .floorInfo(request.floorInfo())
                .description(request.description())
                .tags(request.tags() == null ? new ArrayList<>() : request.tags())
                .has3DModel(false)
                .imageUrls(new ArrayList<>())
                .build();

        managedBroker.addProperty(property);
        Property savedProperty = brokerPropertyRepository.save(property);
        storeInfrastructureAccessibilitiesAfterCommit(savedProperty.getPropertyId());

        return toData(savedProperty);
    }

    private void storeInfrastructureAccessibilitiesAfterCommit(Long propertyId) {
        if (propertyId == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            propertyInfrastructureService.storeInfrastructureAccessibilitiesAsync(propertyId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                propertyInfrastructureService.storeInfrastructureAccessibilitiesAsync(propertyId);
            }
        });
    }

    private BrokerPropertyData toData(Property property) {
        return new BrokerPropertyData(
                property.getPropertyId(),
                property.getTitle(),
                property.getRoomType(),
                property.getAddress(),
                new CoordinateDto(property.getLatitude(), property.getLongitude()),
                property.getTradeType(),
                property.getDeposit(),
                property.getMonthlyRent(),
                property.getMaintenanceFee(),
                property.getAreaM2() == null ? null : property.getAreaM2().doubleValue(),
                property.getFloorInfo(),
                property.getDescription(),
                property.getTags() == null ? List.of() : property.getTags(),
                property.getHas3DModel()
        );
    }
}
