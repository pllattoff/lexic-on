package com.lexicon.backend.security;

import com.lexicon.backend.exception.VerifiedEmailNotFoundException;
import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.service.AppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

// Resolves the AppUser for a sign-in with any OAuth2 provider. Spring Security treats only an
// AuthenticationException as a rejected login, so the "no verified email" failure of the service
// is translated here once, for every provider.
@Component
@RequiredArgsConstructor
public class AppUserResolver {

    private final AppUserService appUserService;

    public AppUser resolve(String provider, String providerUserId, Supplier<Optional<String>> verifiedEmailSupplier) {
        try {
            return appUserService.findOrCreate(provider, providerUserId, verifiedEmailSupplier);
        } catch (VerifiedEmailNotFoundException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("verified_email_not_found"), e.getMessage(), e);
        }
    }
}
