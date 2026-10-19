package com.lexicon.backend.security;

import com.lexicon.backend.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Optional;

// Handles the sign-in with an OpenID Connect provider (Google). Spring Security does not call
// CustomOAuth2UserService for such a provider, because the "openid" scope selects this service instead.
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final AppUserResolver appUserResolver;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        return toAppOidcUser(userRequest, super.loadUser(userRequest));
    }

    // Combine OpenID Connect claims with the corresponding AppUser data
    AppOidcUser toAppOidcUser(OidcUserRequest userRequest, OidcUser googleUser) {
        String provider = userRequest.getClientRegistration().getRegistrationId();

        AppUser appUser = appUserResolver.resolve(provider, googleUser.getSubject(), () -> findVerifiedEmail(googleUser));

        return new AppOidcUser(appUser.getId(), appUser.getEmail(), appUser.getRole(), googleUser);
    }

    // Google states in the email_verified claim whether it has checked the email address
    private Optional<String> findVerifiedEmail(OidcUser googleUser) {
        return Optional.ofNullable(googleUser.getEmail())
                .filter(email -> Boolean.TRUE.equals(googleUser.getEmailVerified()));
    }
}
