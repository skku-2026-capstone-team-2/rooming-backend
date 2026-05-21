package com.skku.zip.domain.broker.controller;

import com.skku.zip.domain.broker.dto.BrokerAdditionalInfoRequest;
import com.skku.zip.domain.broker.dto.BrokerProfileData;
import com.skku.zip.domain.broker.entity.Broker;
import com.skku.zip.domain.broker.service.BrokerProfileService;
import com.skku.zip.security.principal.PrincipalDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrokerAdditionalInfoControllerTest {

    private BrokerProfileService brokerProfileService;
    private MockMvc mockMvc;
    private Broker broker;

    @BeforeEach
    void setUp() {
        brokerProfileService = mock(BrokerProfileService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new BrokerAdditionalInfoController(brokerProfileService))
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
    void brokerCanUploadAdditionalInfoWithOptionalOfficeId() throws Exception {
        MockMultipartFile additionalInfo = new MockMultipartFile(
                "additionalInfo",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                        {
                          "officeId": 12,
                          "registrationNo": "123-45-67890",
                          "phoneNumber": "010-1234-5678"
                        }
                        """.getBytes()
        );
        MockMultipartFile verificationDocument = new MockMultipartFile(
                "verificationDocument",
                "broker-registration.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf".getBytes()
        );
        when(brokerProfileService.updateAdditionalInfo(
                eq(broker),
                any(BrokerAdditionalInfoRequest.class),
                any(MockMultipartFile.class)
        )).thenReturn(profileData());

        mockMvc.perform(multipart("/api/v1/user/broker/me/additional-info")
                        .file(additionalInfo)
                        .file(verificationDocument)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.officeId").value(12))
                .andExpect(jsonPath("$.data.hasVerificationDocument").value(true))
                .andExpect(jsonPath("$.data.isVerified").value(false));
    }

    private BrokerProfileData profileData() {
        return new BrokerProfileData(
                7L,
                "broker@example.test",
                "Broker",
                "BROKER",
                12L,
                "Rooming Realty",
                "123-45-67890",
                "031-123-4567",
                "123 Suwon-ro",
                "010-1234-5678",
                true,
                "broker-registration.pdf",
                false,
                true
        );
    }
}
