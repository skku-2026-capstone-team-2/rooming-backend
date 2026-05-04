package com.skku.zip.security.principal;

import com.skku.zip.domain.user.entity.User;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PrincipalDetails implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    public PrincipalDetails(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes == null ? Map.of() : attributes;
    }


    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <A> A getAttribute(String name) {
        return (A) this.attributes.get(name);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return this.attributes.getOrDefault("name", user.getName()).toString();
    }

    public User getUser() {
        return user;
    }
}
