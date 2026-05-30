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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrokerAdditionalInfoControllerTest {

    private BrokerProfileService brokerProfileService;
    private MockMvc mockMvc;
    private Broker broker;

    @BeforeEach
    void setUp() {
        brokerProfileService = mock(BrokerProfileService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new BrokerAdditionalInfoController(brokerProfileService),
                        new BrokerVerificationDocumentController(brokerProfileService)
                )
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
    void brokerCanUpdateAdditionalInfoWithJsonBody() throws Exception {
        when(brokerProfileService.updateAdditionalInfo(
                eq(broker),
                any(BrokerAdditionalInfoRequest.class)
        )).thenReturn(profileData(false));

        mockMvc.perform(put("/api/v1/user/broker/me/additional-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "officeId": 12,
                                  "registrationNo": "123-45-67890",
                                  "phoneNumber": "010-1234-5678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.officeId").value(12))
                .andExpect(jsonPath("$.data.hasVerificationDocument").value(false))
                .andExpect(jsonPath("$.data.isVerified").value(false));
    }

    @Test
    void brokerCanUploadVerificationDocumentSeparately() throws Exception {
        MockMultipartFile verificationDocument = new MockMultipartFile(
                "verificationDocument",
                "broker-registration.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf".getBytes()
        );
        when(brokerProfileService.updateVerificationDocument(
                eq(broker),
                any(MockMultipartFile.class)
        )).thenReturn(profileData(true));

        mockMvc.perform(multipart("/api/v1/user/broker/me/verification-document")
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

    private BrokerProfileData profileData(boolean hasVerificationDocument) {
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
                hasVerificationDocument,
                hasVerificationDocument ? "broker-registration.pdf" : null,
                false,
                hasVerificationDocument
        );
    }
}
