package com.rooming.security.oauth;

import com.rooming.domain.broker.entity.Broker;
import com.rooming.domain.user.entity.AccountType;
import com.rooming.domain.user.entity.User;
import com.rooming.security.jwt.JwtProvider;
import com.rooming.security.principal.PrincipalDetails;
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
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Value("${app.frontend.redirect-uri:https://rooming-frontend.vercel.app/}")
    private String frontendRedirectUri;

    @Value("${app.frontend.local-redirect-uri:http://localhost:3000/}")
    private String localFrontendRedirectUri;

    @Value("${app.auth.access-token-cookie-name:ROOMING_ACCESS_TOKEN}")
    private String accessTokenCookieName;

    @Value("${app.auth.oauth-account-type-cookie-name:ROOMING_OAUTH_ACCOUNT_TYPE}")
    private String accountTypeCookieName;

    @Value("${app.auth.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${app.auth.cookie-same-site:None}")
    private String cookieSameSite;

    @Value("${app.auth.redirect-access-token:false}")
    private boolean redirectAccessToken;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();
        String accessToken = jwtProvider.createAccessToken(user);

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie(accessToken).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccountTypeCookie().toString());

        String targetUrl = buildTargetUrl(request, user, accessToken);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private ResponseCookie accessTokenCookie(String accessToken) {
        return ResponseCookie.from(accessTokenCookieName, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)
                .maxAge(Duration.ofSeconds(jwtProvider.getAccessTokenValidityInSeconds()))
                .build();
    }

    private ResponseCookie clearAccountTypeCookie() {
        return ResponseCookie.from(accountTypeCookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)
                .maxAge(Duration.ZERO)
                .build();
    }

    private boolean isProfileComplete(User user) {
        if (user.getAccountType() == AccountType.BROKER && user instanceof Broker broker) {
            return broker.isProfileComplete();
        }
        return true;
    }

    private String buildTargetUrl(HttpServletRequest request, User user, String accessToken) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("accountType", user.getAccountType().name());
        params.put("profileComplete", String.valueOf(isProfileComplete(user)));

        String redirectUri = frontendRedirectUri(request);

        if (redirectAccessToken) {
            params.put("accessToken", accessToken);
            return appendFragmentParams(redirectUri, params);
        }

        return appendQueryParams(redirectUri, params);
    }

    private String frontendRedirectUri(HttpServletRequest request) {
        if (isLocalBackendRequest(request)) {
            return localFrontendRedirectUri;
        }
        return frontendRedirectUri;
    }

    private boolean isLocalBackendRequest(HttpServletRequest request) {
        String serverName = request.getServerName();
        if (serverName == null) {
            return false;
        }

        String host = serverName.toLowerCase();
        return host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1")
                || host.equals("[::1]");
    }

    private String appendQueryParams(String url, Map<String, String> params) {
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + encodeParams(params);
    }

    private String appendFragmentParams(String url, Map<String, String> params) {
        String separator = url.contains("#") ? "&" : "#";
        return url + separator + encodeParams(params);
    }

    private String encodeParams(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
