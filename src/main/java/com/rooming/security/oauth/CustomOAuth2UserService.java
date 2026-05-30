package com.rooming.security.oauth;

import com.rooming.domain.broker.entity.Broker;
import com.rooming.domain.broker.repository.BrokerRepository;
import com.rooming.domain.user.entity.AccountType;
import com.rooming.domain.seeker.entity.Seeker;
import com.rooming.domain.user.entity.User;
import com.rooming.domain.seeker.repository.SeekerRepository;
import com.rooming.security.oauth.info.GoogleUserInfo;
import com.rooming.security.oauth.info.OAuth2UserInfo;
import com.rooming.security.principal.PrincipalDetails;
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

    private final SeekerRepository seekerRepository;
    private final BrokerRepository brokerRepository;
    private final OAuth2AccountTypeResolver accountTypeResolver;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuth2UserInfo userInfo = new GoogleUserInfo(attributes);
        String loginId = userInfo.getProvider()+'_'+userInfo.getProviderId();

        AccountType accountType = accountTypeResolver.resolveFromCurrentRequest();
        User user = switch (accountType) {
            case SEEKER -> findOrCreateSeeker(userInfo, loginId);
            case BROKER -> findOrCreateBroker(userInfo, loginId);
        };

        return new PrincipalDetails(user, attributes);
    }

    private Seeker findOrCreateSeeker(OAuth2UserInfo userInfo, String loginId) {
        return seekerRepository.findByLoginId(loginId).orElseGet(() -> seekerRepository.save(
                Seeker.builder()
                        .loginId(loginId)
                        .name(userInfo.getName())
                        .email(userInfo.getEmail())
                        .provider(userInfo.getProvider())
                        .build()
        ));
    }

    private Broker findOrCreateBroker(OAuth2UserInfo userInfo, String loginId) {
        return brokerRepository.findByLoginId(loginId).orElseGet(() -> brokerRepository.save(
                Broker.builder()
                        .loginId(loginId)
                        .name(userInfo.getName())
                        .email(userInfo.getEmail())
                        .provider(userInfo.getProvider())
                        .build()
        ));
    }
}