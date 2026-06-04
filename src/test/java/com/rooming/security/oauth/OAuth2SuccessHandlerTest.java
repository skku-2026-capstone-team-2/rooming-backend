package com.rooming.security.oauth;

import com.rooming.domain.seeker.entity.Seeker;
import com.rooming.security.jwt.JwtProvider;
import com.rooming.security.principal.PrincipalDetails;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2SuccessHandlerTest {

    @Test
    void successfulGoogleLoginFromDeployedBackendRedirectsToConfiguredFrontendUrl() throws Exception {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        when(jwtProvider.createAccessToken(any())).thenReturn("access-token");
        when(jwtProvider.getAccessTokenValidityInSeconds()).thenReturn(86_400L);
        OAuth2SuccessHandler successHandler = new OAuth2SuccessHandler(jwtProvider);
        ReflectionTestUtils.setField(successHandler, "frontendRedirectUri", "https://rooming-frontend.vercel.app/");
        ReflectionTestUtils.setField(successHandler, "localFrontendRedirectUri", "http://localhost:3000/");
        ReflectionTestUtils.setField(successHandler, "accessTokenCookieName", "ROOMING_ACCESS_TOKEN");
        ReflectionTestUtils.setField(successHandler, "accountTypeCookieName", "ROOMING_OAUTH_ACCOUNT_TYPE");
        ReflectionTestUtils.setField(successHandler, "cookieSecure", true);
        ReflectionTestUtils.setField(successHandler, "cookieSameSite", "None");

        Seeker seeker = Seeker.builder()
                .name("Seeker")
                .email("seeker@example.test")
                .provider("google")
                .loginId("google_seeker")
                .build();
        PrincipalDetails principalDetails = new PrincipalDetails(seeker, Map.of("name", seeker.getName()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("rooming.cloud");
        request.setServerPort(443);
        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
                request,
                response,
                new TestingAuthenticationToken(principalDetails, null, principalDetails.getAuthorities())
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://rooming-frontend.vercel.app/?accountType=SEEKER&profileComplete=true");
        assertThat(response.getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie)
                        .contains("ROOMING_ACCESS_TOKEN=access-token")
                        .contains("SameSite=None")
                        .contains("Secure"));
    }

    @Test
    void successfulGoogleLoginFromLocalBackendRedirectsToLocalFrontendUrl() throws Exception {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        when(jwtProvider.createAccessToken(any())).thenReturn("access-token");
        when(jwtProvider.getAccessTokenValidityInSeconds()).thenReturn(86_400L);
        OAuth2SuccessHandler successHandler = new OAuth2SuccessHandler(jwtProvider);
        ReflectionTestUtils.setField(successHandler, "frontendRedirectUri", "https://rooming-frontend.vercel.app/");
        ReflectionTestUtils.setField(successHandler, "localFrontendRedirectUri", "http://localhost:3000/");
        ReflectionTestUtils.setField(successHandler, "accessTokenCookieName", "ROOMING_ACCESS_TOKEN");
        ReflectionTestUtils.setField(successHandler, "accountTypeCookieName", "ROOMING_OAUTH_ACCOUNT_TYPE");
        ReflectionTestUtils.setField(successHandler, "cookieSecure", false);
        ReflectionTestUtils.setField(successHandler, "cookieSameSite", "Lax");

        Seeker seeker = Seeker.builder()
                .name("Seeker")
                .email("seeker@example.test")
                .provider("google")
                .loginId("google_seeker")
                .build();
        PrincipalDetails principalDetails = new PrincipalDetails(seeker, Map.of("name", seeker.getName()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
                request,
                response,
                new TestingAuthenticationToken(principalDetails, null, principalDetails.getAuthorities())
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/?accountType=SEEKER&profileComplete=true");
    }

    @Test
    void canRedirectAccessTokenInFragmentWhenConfigured() throws Exception {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        when(jwtProvider.createAccessToken(any())).thenReturn("access-token");
        when(jwtProvider.getAccessTokenValidityInSeconds()).thenReturn(86_400L);
        OAuth2SuccessHandler successHandler = new OAuth2SuccessHandler(jwtProvider);
        ReflectionTestUtils.setField(successHandler, "frontendRedirectUri", "https://rooming-frontend.vercel.app/");
        ReflectionTestUtils.setField(successHandler, "localFrontendRedirectUri", "http://localhost:3000/");
        ReflectionTestUtils.setField(successHandler, "accessTokenCookieName", "ROOMING_ACCESS_TOKEN");
        ReflectionTestUtils.setField(successHandler, "accountTypeCookieName", "ROOMING_OAUTH_ACCOUNT_TYPE");
        ReflectionTestUtils.setField(successHandler, "cookieSecure", true);
        ReflectionTestUtils.setField(successHandler, "cookieSameSite", "None");
        ReflectionTestUtils.setField(successHandler, "redirectAccessToken", true);

        Seeker seeker = Seeker.builder()
                .name("Seeker")
                .email("seeker@example.test")
                .provider("google")
                .loginId("google_seeker")
                .build();
        PrincipalDetails principalDetails = new PrincipalDetails(seeker, Map.of("name", seeker.getName()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("rooming.cloud");
        request.setServerPort(443);
        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
                request,
                response,
                new TestingAuthenticationToken(principalDetails, null, principalDetails.getAuthorities())
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://rooming-frontend.vercel.app/#accountType=SEEKER&profileComplete=true&accessToken=access-token");
    }
}
