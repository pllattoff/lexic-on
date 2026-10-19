package com.lexicon.backend.security;

import com.lexicon.backend.client.GithubApiClient;
import com.lexicon.backend.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AppUserResolver appUserResolver;
    private final GithubApiClient githubApiClient;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        return toAppOAuth2User(userRequest, super.loadUser(userRequest));
    }

    // Combine OAuth2 provider data with the corresponding AppUser data
    AppOAuth2User toAppOAuth2User(OAuth2UserRequest userRequest, OAuth2User githubUser) {
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerUserId = String.valueOf(githubUser.getAttributes().get("id"));

        // The email comes only from GitHub's emails endpoint: the email in the profile carries no
        // information about whether it is verified
        AppUser appUser = appUserResolver.resolve(provider, providerUserId,
                () -> githubApiClient.findPrimaryVerifiedEmail(userRequest.getAccessToken().getTokenValue()));

        return new AppOAuth2User(appUser.getId(), appUser.getEmail(), appUser.getRole(), githubUser.getAttributes());
    }
}