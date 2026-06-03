package com.rooming.domain.broker.controller;

import com.rooming.domain.broker.dto.BrokerPropertyData;
import com.rooming.domain.broker.dto.BrokerPropertySummaryData;
import com.rooming.domain.broker.entity.Broker;
import com.rooming.domain.broker.service.BrokerPropertyService;
import com.rooming.domain.locations.dto.CoordinateDto;
import com.rooming.domain.property.entity.TradeType;
import com.rooming.security.principal.PrincipalDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

class BrokerPropertyControllerTest {

    private BrokerPropertyService brokerPropertyService;
    private MockMvc mockMvc;
    private Broker broker;

    @BeforeEach
    void setUp() {
        brokerPropertyService = mock(BrokerPropertyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new BrokerPropertyController(brokerPropertyService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        broker = Broker.builder()
                .name("Broker")
                .email("broker@example.test")
                .provider("google")
                .loginId("google_broker")
                .build();
        PrincipalDetails principalDetails = new PrincipalDetails(broker, Map.of("name", broker.getName()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken(
                principalDetails,
                null,
                principalDetails.getAuthorities()
        ));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void brokerCanFetchRegisteredPropertySummaries() throws Exception {
        when(brokerPropertyService.getMyPropertySummaries(eq(broker)))
                .thenReturn(List.of(new BrokerPropertySummaryData(11L, "Verified broker property")));

        mockMvc.perform(get("/api/v1/broker/me/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].propertyId").value(11))
                .andExpect(jsonPath("$.data[0].title").value("Verified broker property"))
                .andExpect(jsonPath("$.data[0].roadAddress").doesNotExist())
                .andExpect(jsonPath("$.data[0].depositAmount").doesNotExist())
                .andExpect(jsonPath("$.message").value("Broker property list fetched."));
    }

    @Test
    void existingBrokerPropertyCreateRouteStillWorks() throws Exception {
        when(brokerPropertyService.createProperty(eq(broker), org.mockito.ArgumentMatchers.any()))
                .thenReturn(propertyData());

        mockMvc.perform(post("/api/v1/user/broker/me/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Verified broker property",
                                  "propertyType": "one_room",
                                  "tradeType": "MONTHLY_RENT",
                                  "depositAmount": 500,
                                  "monthlyRent": 55,
                                  "tags": ["quiet", "campus"],
                                  "maintenanceFee": 5,
                                  "areaM2": 23.5,
                                  "floorInfo": "3F",
                                  "roomCount": 1,
                                  "bathroomCount": 1,
                                  "direction": "SOUTH",
                                  "availableFrom": "2026-06-01",
                                  "description": "Near campus",
                                  "roadAddress": "123 Suwon-ro",
                                  "location": {
                                    "latitude": 37.2945,
                                    "longitude": 126.9748
                                  },
                                  "splineUrl": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.propertyId").value(11));
    }

    private BrokerPropertyData propertyData() {
        return new BrokerPropertyData(
                11L,
                "Verified broker property",
                "one_room",
                "123 Suwon-ro",
                new CoordinateDto(37.2945, 126.9748),
                TradeType.MONTHLY_RENT,
                500,
                55,
                5,
                23.5,
                "3F",
                "Near campus",
                List.of("quiet", "campus"),
                false,
                null
        );
    }
}
