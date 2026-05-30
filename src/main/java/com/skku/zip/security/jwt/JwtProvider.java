package com.skku.zip.security.jwt;

import com.skku.zip.domain.user.entity.AccountType;
import com.skku.zip.domain.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    private static final String ACCOUNT_TYPE_CLAIM = "accountType";

    @Value("${jwt.secret}")
    private String secretKeyString;

    @Value("${jwt.access-token-validity-in-seconds}")
    private long accessTokenValidityInSeconds;

    private Key secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    }

    public String createAccessToken(User user) {
        return createAccessToken(user.getId(), user.getAccountType());
    }

    public String createAccessToken(Long userId, AccountType accountType) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + (accessTokenValidityInSeconds * 1000));

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(ACCOUNT_TYPE_CLAIM, accountType.name())
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public AccountType getAccountTypeFromToken(String token) {
        return parseAccountType(getClaims(token));
    }

    public long getAccessTokenValidityInSeconds() {
        return accessTokenValidityInSeconds;
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            parseAccountType(claims);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token.");
        } catch (JwtException e) {
            log.error("Invalid JWT token.");
        } catch (IllegalArgumentException e) {
            log.error("JWT token is empty or has invalid claims.");
        }
        return false;
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private AccountType parseAccountType(Claims claims) {
        String accountType = claims.get(ACCOUNT_TYPE_CLAIM, String.class);
        if (accountType == null || accountType.isBlank()) {
            throw new IllegalArgumentException("JWT account type claim is missing.");
        }
        return AccountType.valueOf(accountType);
    }
}
