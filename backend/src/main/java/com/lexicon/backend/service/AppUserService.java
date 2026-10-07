package com.lexicon.backend.service;

import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.model.UserIdentity;
import com.lexicon.backend.repository.AppUserRepository;
import com.lexicon.backend.repository.UserIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final UserIdentityRepository userIdentityRepository;

    // Pass a callback that supplies the email on demand when creating a new user.
    // This avoids an unnecessary call to the OAuth2 provider for existing users.
    @Transactional
    public AppUser findOrCreate(String provider, String providerUserId, Supplier<String> emailSupplier) {
        return userIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(UserIdentity::getAppUser)
                .orElseGet(() -> createAppUser(provider, providerUserId, emailSupplier.get()));
    }

    private AppUser createAppUser(String provider, String providerUserId, String email) {
        AppUser appUser = appUserRepository.save(new AppUser(email));
        userIdentityRepository.save(new UserIdentity(appUser, provider, providerUserId));
        return appUser;
    }
}