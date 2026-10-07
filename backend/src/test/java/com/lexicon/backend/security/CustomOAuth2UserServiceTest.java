package com.lexicon.backend.security;

import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private AppUserService appUserService;

    @Mock
    private GithubApiClient githubApiClient;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    void toAppOAuth2User_usesEmailFromProfile_whenProfileHasEmail() {
        // GIVEN a GitHub profile with a public email
        givenAppUserServiceCreatesUserWithResolvedEmail();

        // WHEN
        AppOAuth2User user = customOAuth2UserService.toAppOAuth2User(userRequest(), githubUser("test@example.com"));

        // THEN the profile email is used and the GitHub emails endpoint is not called
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        verifyNoInteractions(githubApiClient);
    }

    @Test
    void toAppOAuth2User_fallsBackToGithubEmails_whenProfileEmailIsPrivate() {
        // GIVEN a GitHub profile with a private email (null attribute)
        givenAppUserServiceCreatesUserWithResolvedEmail();
        when(githubApiClient.findPrimaryVerifiedEmail("token-value")).thenReturn(Optional.of("private@example.com"));

        // WHEN
        AppOAuth2User user = customOAuth2UserService.toAppOAuth2User(userRequest(), githubUser(null));

        // THEN the primary verified email from the emails endpoint is used
        assertThat(user.getEmail()).isEqualTo("private@example.com");
    }

    @Test
    void toAppOAuth2User_throwsOAuth2AuthenticationException_whenAccountHasNoVerifiedEmail() {
        // GIVEN a private profile email and no verified email on the account
        givenAppUserServiceCreatesUserWithResolvedEmail();
        when(githubApiClient.findPrimaryVerifiedEmail("token-value")).thenReturn(Optional.empty());
        OAuth2UserRequest userRequest = userRequest();
        OAuth2User githubUser = githubUser(null);

        // WHEN
        assertThatThrownBy(() -> customOAuth2UserService.toAppOAuth2User(userRequest, githubUser))
        // THEN the login is rejected
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessage("GitHub account has no verified email");
    }

    @Test
    void toAppOAuth2User_keepsGithubAttributesAndGrantsRoleUser_whenUserIsResolved() {
        // GIVEN
        givenAppUserServiceCreatesUserWithResolvedEmail();

        // WHEN
        AppOAuth2User user = customOAuth2UserService.toAppOAuth2User(userRequest(), githubUser("test@example.com"));

        // THEN the GitHub attributes stay available, and the user has the role from AppUser
        assertThat(user.<String>getAttribute("login")).isEqualTo("testuser");
        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    // The real AppUserService resolves the email only for a new user; the stub mimics a new user
    private void givenAppUserServiceCreatesUserWithResolvedEmail() {
        when(appUserService.findOrCreate(eq("github"), eq("42"), any())).thenAnswer(invocation -> {
            Supplier<String> emailSupplier = invocation.getArgument(2);
            return new AppUser(emailSupplier.get());
        });
    }

    private OAuth2UserRequest userRequest() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("github")
                .clientId("test-id")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/callback")
                .authorizationUri("http://localhost/authorize")
                .tokenUri("http://localhost/token")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "token-value", Instant.now(), Instant.now().plusSeconds(60));

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }

    // email is null for GitHub accounts with a private email
    private OAuth2User githubUser(String email) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 42);
        attributes.put("login", "testuser");
        attributes.put("email", email);

        return new DefaultOAuth2User(List.of(), attributes, "id");
    }
}