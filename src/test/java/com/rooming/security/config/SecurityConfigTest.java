package com.rooming.security.config;

import com.rooming.security.exception.RestAccessDeniedHandler;
import com.rooming.security.exception.RestAuthenticationEntryPoint;
import com.rooming.security.jwt.JwtAuthenticationFilter;
import com.rooming.security.oauth.CustomOAuth2UserService;
import com.rooming.security.oauth.OAuth2SuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void corsAllowsProductionAndLocalFrontendOriginsWithCredentials() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(CustomOAuth2UserService.class),
                mock(OAuth2SuccessHandler.class),
                mock(JwtAuthenticationFilter.class),
                mock(RestAuthenticationEntryPoint.class),
                mock(RestAccessDeniedHandler.class)
        );
        ReflectionTestUtils.setField(
                securityConfig,
                "frontendAllowedOrigins",
                "https://rooming-frontend.vercel.app/, http://localhost:5173/"
        );

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("https://rooming-frontend.vercel.app"))
                .isEqualTo("https://rooming-frontend.vercel.app");
        assertThat(configuration.checkOrigin("http://localhost:5173"))
                .isEqualTo("http://localhost:5173");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }
}