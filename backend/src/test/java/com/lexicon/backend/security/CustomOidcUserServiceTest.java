package com.lexicon.backend.security;

import com.lexicon.backend.model.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

    @Mock
    private AppUserResolver appUserResolver;

    @Captor
    private ArgumentCaptor<Supplier<Optional<String>>> verifiedEmailSupplier;

    @InjectMocks
    private CustomOidcUserService customOidcUserService;

    @Test
    void toAppOidcUser_suppliesEmail_whenEmailIsVerified() {
        // GIVEN a Google account with a verified email
        givenResolverFindsUser();

        // WHEN
        customOidcUserService.toAppOidcUser(userRequest(), googleUser("test@example.com", true));

        // THEN the Google ID is the provider user ID, and the verified email is offered for a new identity
        verify(appUserResolver).resolve(eq("google"), eq("1092"), verifiedEmailSupplier.capture());
        assertThat(verifiedEmailSupplier.getValue().get()).contains("test@example.com");
    }

    @Test
    void toAppOidcUser_suppliesNoEmail_whenEmailIsNotVerified() {
        // GIVEN a Google account whose email is not verified
        givenResolverFindsUser();

        // WHEN
        customOidcUserService.toAppOidcUser(userRequest(), googleUser("test@example.com", false));

        // THEN no email is offered
        verify(appUserResolver).resolve(eq("google"), eq("1092"), verifiedEmailSupplier.capture());
        assertThat(verifiedEmailSupplier.getValue().get()).isEmpty();
    }

    @Test
    void toAppOidcUser_suppliesNoEmail_whenEmailIsMissing() {
        // GIVEN a Google account without an email
        givenResolverFindsUser();

        // WHEN
        customOidcUserService.toAppOidcUser(userRequest(), googleUser(null, true));

        // THEN no email is offered
        verify(appUserResolver).resolve(eq("google"), eq("1092"), verifiedEmailSupplier.capture());
        assertThat(verifiedEmailSupplier.getValue().get()).isEmpty();
    }

    @Test
    void toAppOidcUser_grantsRoleUserAndUsesEmailOfAppUser_whenUserIsResolved() {
        // GIVEN
        givenResolverFindsUser();

        // WHEN
        AppOidcUser user = customOidcUserService.toAppOidcUser(userRequest(), googleUser("test@example.com", true));

        // THEN the user has the role and the email from AppUser
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    private void givenResolverFindsUser() {
        when(appUserResolver.resolve(eq("google"), eq("1092"), any())).thenReturn(new AppUser("test@example.com"));
    }

    private OidcUserRequest userRequest() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
                .clientId("test-id")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/callback")
                .authorizationUri("http://localhost/authorize")
                .tokenUri("http://localhost/token")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "token-value", Instant.now(), Instant.now().plusSeconds(60));
        OidcIdToken idToken = new OidcIdToken(
                "id-token-value", Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", "1092"));

        return new OidcUserRequest(clientRegistration, accessToken, idToken);
    }

    // email is null for a Google account without an email
    private OidcUser googleUser(String email, boolean emailVerified) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "1092");
        claims.put("email", email);
        claims.put("email_verified", emailVerified);
        OidcIdToken idToken = new OidcIdToken(
                "id-token-value", Instant.now(), Instant.now().plusSeconds(60), claims);

        return new DefaultOidcUser(List.of(), idToken);
    }
}
