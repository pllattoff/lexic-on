package com.lexicon.backend.service;

import com.lexicon.backend.exception.VerifiedEmailNotFoundException;
import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.model.UserIdentity;
import com.lexicon.backend.repository.AppUserRepository;
import com.lexicon.backend.repository.UserIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final UserIdentityRepository userIdentityRepository;

    // Pass a callback that supplies the provider-verified email on demand (empty when there is none).
    // It is only called for an identity that is not stored yet, which avoids an unnecessary call
    // to the OAuth2 provider for known users.
    @Transactional
    public AppUser findOrCreate(String provider, String providerUserId, Supplier<Optional<String>> verifiedEmailSupplier) {
        return userIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(UserIdentity::getAppUser)
                .orElseGet(() -> addIdentity(provider, providerUserId, verifiedEmailSupplier.get()));
    }

    // A new identity joins the AppUser that owns its verified email, or gets a new AppUser.
    // Only a verified email may link identities, otherwise someone could take over an account
    // by signing up at a provider with an email address that is not theirs.
    private AppUser addIdentity(String provider, String providerUserId, Optional<String> verifiedEmail) {
        String email = verifiedEmail.orElseThrow(
                () -> new VerifiedEmailNotFoundException("The account has no verified email"));
        AppUser appUser = appUserRepository.findByEmail(email)
                .orElseGet(() -> appUserRepository.save(new AppUser(email)));
        userIdentityRepository.save(new UserIdentity(appUser, provider, providerUserId));
        return appUser;
    }
}