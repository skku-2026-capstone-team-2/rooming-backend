package com.skku.zip.security.oauth;

import com.skku.zip.domain.user.entity.User;
import com.skku.zip.domain.user.repository.UserRepository;
import com.skku.zip.security.oauth.user.GoogleUserInfo;
import com.skku.zip.security.oauth.user.OAuth2UserInfo;
import com.skku.zip.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuth2UserInfo userInfo = new GoogleUserInfo(attributes);
        String loginId = userInfo.getProvider()+'_'+userInfo.getProviderId();

        User user = userRepository.findByLoginId(loginId).orElseGet(() -> {
            User newUser = User.builder()
                    .loginId(loginId)
                    .name(userInfo.getName())
                    .email(userInfo.getEmail())
                    .provider(userInfo.getProvider())
                    .build();
            userRepository.save(newUser);
            return newUser;
        });

        return new PrincipalDetails(user, attributes);
    }
}
