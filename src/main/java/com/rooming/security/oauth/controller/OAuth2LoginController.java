package com.rooming.security.oauth.controller;

import com.rooming.domain.user.entity.AccountType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class OAuth2LoginController {

    @Value("${app.auth.oauth-account-type-cookie-name:ROOMING_OAUTH_ACCOUNT_TYPE}")
    private String accountTypeCookieName;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.auth.cookie-same-site:Lax}")
    private String cookieSameSite;

    @GetMapping("/seeker/google")
    public ResponseEntity<Void> loginSeekerWithGoogle() {
        return redirectToGoogle(AccountType.SEEKER);
    }

    @GetMapping("/broker/google")
    public ResponseEntity<Void> loginBrokerWithGoogle() {
        return redirectToGoogle(AccountType.BROKER);
    }

    private ResponseEntity<Void> redirectToGoogle(AccountType accountType) {
        ResponseCookie cookie = ResponseCookie.from(accountTypeCookieName, accountType.name())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)
                .maxAge(Duration.ofMinutes(5))
                .build();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .location(URI.create("/oauth2/authorization/google"))
                .build();
    }
}