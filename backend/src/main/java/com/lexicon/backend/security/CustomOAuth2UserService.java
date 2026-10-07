package com.lexicon.backend.security;

import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.service.AppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AppUserService appUserService;
    private final GithubApiClient githubApiClient;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        return toAppOAuth2User(userRequest, super.loadUser(userRequest));
    }

    // Combine OAuth2 provider data with the corresponding AppUser data
    AppOAuth2User toAppOAuth2User(OAuth2UserRequest userRequest, OAuth2User githubUser) {
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerUserId = String.valueOf(githubUser.getAttributes().get("id"));

        AppUser appUser = appUserService.findOrCreate(
                provider, providerUserId, () -> resolveEmail(userRequest, githubUser));

        return new AppOAuth2User(appUser.getId(), appUser.getEmail(), appUser.getRole(), githubUser.getAttributes());
    }

    // Use the email provided by GitHub, falling back to GitHub's email API for private emails
    private String resolveEmail(OAuth2UserRequest userRequest, OAuth2User githubUser) {
        String email = githubUser.getAttribute("email");
        if (email != null) {
            return email;
        }

        return githubApiClient.findPrimaryVerifiedEmail(userRequest.getAccessToken().getTokenValue())
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("email_not_found"), "GitHub account has no verified email"));
    }
}