package com.skku.zip.domain.broker.service;

import com.skku.zip.domain.broker.dto.BrokerPropertyCreateRequest;
import com.skku.zip.domain.broker.entity.Broker;
import com.skku.zip.domain.broker.repository.BrokerPropertyRepository;
import com.skku.zip.domain.broker.repository.BrokerRepository;
import com.skku.zip.domain.locations.dto.CoordinateDto;
import com.skku.zip.domain.locations.service.PropertyInfrastructureService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
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
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Broker verification is required before posting a property.");

        verifyNoInteractions(brokerPropertyRepository, propertyInfrastructureService);
    }

    private BrokerPropertyCreateRequest createRequest() {
        return new BrokerPropertyCreateRequest(
                "Verified broker property",
                "one_room",
                "MONTHLY_RENT",
                500,
                55,
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
