package com.lexicon.backend.security;

import com.lexicon.backend.client.GithubApiClient;
import com.lexicon.backend.model.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private AppUserResolver appUserResolver;

    @Mock
    private GithubApiClient githubApiClient;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    void toAppOAuth2User_usesVerifiedEmailFromGithubEmails_whenIdentityIsNew() {
        // GIVEN a new identity, and a GitHub profile whose email says nothing about being verified
        givenResolverCreatesUserWithSuppliedEmail();
        when(githubApiClient.findPrimaryVerifiedEmail("token-value")).thenReturn(Optional.of("verified@example.com"));

        // WHEN
        AppOAuth2User user = customOAuth2UserService.toAppOAuth2User(userRequest(), githubUser("profile@example.com"));

        // THEN the primary verified email from the emails endpoint is used, not the profile email
        assertThat(user.getEmail()).isEqualTo("verified@example.com");
    }

    @Test
    void toAppOAuth2User_doesNotCallGithubEmails_whenIdentityIsKnown() {
        // GIVEN the resolver finds the user without asking for the email
        when(appUserResolver.resolve(eq("github"), eq("42"), any())).thenReturn(new AppUser("test@example.com"));

        // WHEN
        AppOAuth2User user = customOAuth2UserService.toAppOAuth2User(userRequest(), githubUser("test@example.com"));

        // THEN the GitHub emails endpoint is not called
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        verifyNoInteractions(githubApiClient);
    }

    @Test
    void toAppOAuth2User_keepsGithubAttributesAndGrantsRoleUser_whenUserIsResolved() {
        // GIVEN
        givenResolverCreatesUserWithSuppliedEmail();
        when(githubApiClient.findPrimaryVerifiedEmail("token-value")).thenReturn(Optional.of("test@example.com"));

        // WHEN
        AppOAuth2User user = customOAuth2UserService.toAppOAuth2User(userRequest(), githubUser("test@example.com"));

        // THEN the GitHub attributes stay available, and the user has the role from AppUser
        assertThat(user.<String>getAttribute("login")).isEqualTo("testuser");
        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    // The real resolver asks for the email only for a new identity; the stub mimics a new identity
    private void givenResolverCreatesUserWithSuppliedEmail() {
        when(appUserResolver.resolve(eq("github"), eq("42"), any())).thenAnswer(invocation -> {
            Supplier<Optional<String>> verifiedEmailSupplier = invocation.getArgument(2);
            return new AppUser(verifiedEmailSupplier.get().orElseThrow());
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