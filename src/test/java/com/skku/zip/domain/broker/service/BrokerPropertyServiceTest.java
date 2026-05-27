package com.skku.zip.domain.broker.service;

import com.skku.zip.common.exception.ForbiddenException;
import com.skku.zip.domain.broker.dto.BrokerPropertyCreateRequest;
import com.skku.zip.domain.broker.dto.BrokerPropertyData;
import com.skku.zip.domain.broker.entity.Broker;
import com.skku.zip.domain.broker.repository.BrokerPropertyRepository;
import com.skku.zip.domain.broker.repository.BrokerRepository;
import com.skku.zip.domain.locations.dto.CoordinateDto;
import com.skku.zip.domain.locations.service.PropertyInfrastructureService;
import com.skku.zip.domain.property.entity.Property;
import com.skku.zip.domain.property.entity.TradeType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BrokerPropertyServiceTest {

    private final BrokerPropertyRepository brokerPropertyRepository = mock(BrokerPropertyRepository.class);
    private final BrokerRepository brokerRepository = mock(BrokerRepository.class);
    private final PropertyInfrastructureService propertyInfrastructureService = mock(PropertyInfrastructureService.class);
    private final BrokerPropertyService brokerPropertyService = new BrokerPropertyService(
            brokerPropertyRepository,
            brokerRepository,
            propertyInfrastructureService
    );

    @Test
    void unverifiedBrokerCannotCreateProperty() {
        Broker broker = Broker.builder()
                .name("Broker")
                .email("broker@example.test")
                .provider("google")
                .loginId("google_broker")
                .build();
        ReflectionTestUtils.setField(broker, "id", 7L);
        when(brokerRepository.findById(7L)).thenReturn(Optional.of(broker));

        assertThatThrownBy(() -> brokerPropertyService.createProperty(broker, createRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Broker verification is required before posting a property.");

        verifyNoInteractions(brokerPropertyRepository, propertyInfrastructureService);
    }

    @Test
    void depositBasisPropertyHasZeroMonthlyRent() {
        Broker broker = verifiedBroker();
        when(brokerRepository.findById(7L)).thenReturn(Optional.of(broker));
        when(brokerPropertyRepository.save(any(Property.class))).thenAnswer(invocation -> {
            Property property = invocation.getArgument(0);
            ReflectionTestUtils.setField(property, "propertyId", 11L);
            return property;
        });

        BrokerPropertyData data = brokerPropertyService.createProperty(
                broker,
                createRequest(TradeType.DEPOSIT_BASIS, 80)
        );

        assertThat(data.tradeType()).isEqualTo(TradeType.DEPOSIT_BASIS);
        assertThat(data.depositAmount()).isEqualTo(500);
        assertThat(data.monthlyRent()).isZero();
        verify(propertyInfrastructureService).storeInfrastructureAccessibilitiesAsync(anyLong());
    }

    private Broker verifiedBroker() {
        Broker broker = Broker.builder()
                .name("Broker")
                .email("broker@example.test")
                .provider("google")
                .loginId("google_broker")
                .build();
        ReflectionTestUtils.setField(broker, "id", 7L);
        ReflectionTestUtils.setField(broker, "isVerified", true);
        return broker;
    }

    private BrokerPropertyCreateRequest createRequest() {
        return createRequest(TradeType.MONTHLY_RENT, 55);
    }

    private BrokerPropertyCreateRequest createRequest(TradeType tradeType, Integer monthlyRent) {
        return new BrokerPropertyCreateRequest(
                "Verified broker property",
                "one_room",
                tradeType,
                500,
                monthlyRent,
                List.of("quiet", "campus"),
                5,
                23.5,
                "3F",
                1,
                1,
                "SOUTH",
                "2026-06-01",
                "Near campus",
                "123 Suwon-ro",
                new CoordinateDto(37.2945, 126.9748)
        );
    }
}
