package com.skku.zip.security.oauth;

import com.skku.zip.domain.user.entity.AccountType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class OAuth2AccountTypeResolver {

    @Value("${app.auth.oauth-account-type-cookie-name:ROOMING_OAUTH_ACCOUNT_TYPE}")
    private String accountTypeCookieName;

    public AccountType resolveFromCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return AccountType.SEEKER;
        }
        return resolve(attributes.getRequest());
    }

    public AccountType resolve(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return AccountType.SEEKER;
        }

        for (Cookie cookie : request.getCookies()) {
            if (accountTypeCookieName.equals(cookie.getName())) {
                try {
                    return AccountType.valueOf(cookie.getValue());
                } catch (IllegalArgumentException ignored) {
                    return AccountType.SEEKER;
                }
            }
        }
        return AccountType.SEEKER;
    }
}
