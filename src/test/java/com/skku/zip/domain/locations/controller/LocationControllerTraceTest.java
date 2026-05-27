package com.skku.zip.domain.locations.controller;

import com.skku.zip.domain.locations.dto.CoordinateDto;
import com.skku.zip.domain.locations.dto.TargetPlaceCreateRequest;
import com.skku.zip.domain.locations.dto.TargetPlaceResponseItem;
import com.skku.zip.domain.locations.entity.type.PLACE_CATEGORY;
import com.skku.zip.domain.locations.service.TargetPlaceApiService;
import com.skku.zip.domain.seeker.controller.TargetPlaceController;
import com.skku.zip.domain.seeker.entity.Seeker;
import com.skku.zip.security.jwt.JwtAuthenticationFilter;
import com.skku.zip.security.principal.PrincipalDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TargetPlaceController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class LocationControllerTraceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TargetPlaceApiService targetPlaceApiService;

    private Seeker seeker;

    @BeforeEach
    void setUpSeekerPrincipal() {
        seeker = Seeker.builder()
                .name("Location trace seeker")
                .email("location-trace@example.test")
                .provider("google")
                .loginId("location-trace-login")
                .build();

        PrincipalDetails principalDetails = new PrincipalDetails(seeker, Map.of("name", seeker.getName()));
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
    void targetPlaceCreateShowsFrontendRequestAndFrontendResponse() throws Exception {
        String requestJson = """
                {
                  "category": "SCHOOL",
                  "placeName": "Sungkyunkwan University",
                  "roadAddress": "2066 Seobu-ro, Jangan-gu, Suwon",
                  "location": {
                    "latitude": 37.2945,
                    "longitude": 126.9748
                  },
                  "memo": "Morning classes"
                }
                """;
        when(targetPlaceApiService.createTargetPlace(eq(seeker), any(TargetPlaceCreateRequest.class)))
                .thenReturn(new TargetPlaceResponseItem(
                        29L,
                        "SCHOOL",
                        "Sungkyunkwan University",
                        "2066 Seobu-ro, Jangan-gu, Suwon",
                        new CoordinateDto(37.2945, 126.9748),
                        "Morning classes"
                ));

        MvcResult result = mockMvc.perform(post("/api/v1/user/seeker/target-place")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Target place saved."))
                .andExpect(jsonPath("$.data.targetPlaceId").value(29))
                .andExpect(jsonPath("$.data.category").value("SCHOOL"))
                .andExpect(jsonPath("$.data.placeName").value("Sungkyunkwan University"))
                .andExpect(jsonPath("$.data.location.latitude").value(37.2945))
                .andExpect(jsonPath("$.data.memo").value("Morning classes"))
                .andReturn();

        printExchange(
                "Frontend -> Java target place create request",
                "POST /api/v1/user/seeker/target-place",
                requestJson,
                result
        );

        ArgumentCaptor<TargetPlaceCreateRequest> requestCaptor =
                ArgumentCaptor.forClass(TargetPlaceCreateRequest.class);
        verify(targetPlaceApiService).createTargetPlace(eq(seeker), requestCaptor.capture());
        assertThat(requestCaptor.getValue().category()).isEqualTo(PLACE_CATEGORY.SCHOOL);
        assertThat(requestCaptor.getValue().location())
                .isEqualTo(new CoordinateDto(37.2945, 126.9748));
    }

    private void printExchange(
            String requestTitle,
            String requestLine,
            String requestBody,
            MvcResult result
    ) throws Exception {
        System.out.println();
        System.out.println("=== " + requestTitle + " ===");
        System.out.println(requestLine);
        if (requestBody != null) {
            System.out.println(requestBody);
        }
        System.out.println("=== Java location API -> frontend response ===");
        System.out.println(result.getResponse().getContentAsString());
    }
}
