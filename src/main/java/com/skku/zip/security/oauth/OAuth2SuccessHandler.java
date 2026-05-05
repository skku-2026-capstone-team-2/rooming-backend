package com.skku.zip.security.oauth;

import com.skku.zip.domain.broker.entity.Broker;
import com.skku.zip.domain.user.entity.AccountType;
import com.skku.zip.domain.user.entity.User;
import com.skku.zip.security.jwt.JwtProvider;
import com.skku.zip.security.principal.PrincipalDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Value("${app.frontend.redirect-uri:http://localhost:3000/oauth2/redirect}")
    private String frontendRedirectUri;

    @Value("${app.auth.access-token-cookie-name:ROOMING_ACCESS_TOKEN}")
    private String accessTokenCookieName;

    @Value("${app.auth.oauth-account-type-cookie-name:ROOMING_OAUTH_ACCOUNT_TYPE}")
    private String accountTypeCookieName;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();
        String accessToken = jwtProvider.createAccessToken(user);

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie(accessToken).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccountTypeCookie().toString());

        String targetUrl = frontendRedirectUri
                + "?accountType=" + encode(user.getAccountType().name())
                + "&profileComplete=" + isProfileComplete(user);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private ResponseCookie accessTokenCookie(String accessToken) {
        return ResponseCookie.from(accessTokenCookieName, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(jwtProvider.getAccessTokenValidityInSeconds()))
                .build();
    }

    private ResponseCookie clearAccountTypeCookie() {
        return ResponseCookie.from(accountTypeCookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
    }

    private boolean isProfileComplete(User user) {
        if (user.getAccountType() == AccountType.BROKER && user instanceof Broker broker) {
            return broker.isProfileComplete();
        }
        return true;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
