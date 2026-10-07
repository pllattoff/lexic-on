package com.lexicon.backend.service;

import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.model.UserIdentity;
import com.lexicon.backend.repository.AppUserRepository;
import com.lexicon.backend.repository.UserIdentityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    void findOrCreate_createsUserAndIdentity_whenIdentityIsNew() {
        // GIVEN no stored identity for this GitHub account
        when(userIdentityRepository.findByProviderAndProviderUserId("github", "42")).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).then(returnsFirstArg());

        // WHEN
        AppUser appUser = appUserService.findOrCreate("github", "42", () -> "test@example.com");

        // THEN a user with the resolved email is stored
        assertThat(appUser.getEmail()).isEqualTo("test@example.com");
        verify(appUserRepository).save(appUser);

        // AND an identity linking this GitHub account to that user
        ArgumentCaptor<UserIdentity> savedIdentity = ArgumentCaptor.forClass(UserIdentity.class);
        verify(userIdentityRepository).save(savedIdentity.capture());
        assertThat(savedIdentity.getValue().getAppUser()).isSameAs(appUser);
        assertThat(savedIdentity.getValue().getProvider()).isEqualTo("github");
        assertThat(savedIdentity.getValue().getProviderUserId()).isEqualTo("42");
    }

    @Test
    void findOrCreate_returnsExistingUser_whenIdentityAlreadyExists() {
        // GIVEN a user that has logged in before
        AppUser existing = new AppUser("test@example.com");
        when(userIdentityRepository.findByProviderAndProviderUserId("github", "42"))
                .thenReturn(Optional.of(new UserIdentity(existing, "github", "42")));

        // WHEN the same GitHub account logs in again
        AppUser appUser = appUserService.findOrCreate("github", "42", () -> "test@example.com");

        // THEN the existing user is returned and nothing new is stored
        assertThat(appUser).isSameAs(existing);
        verifyNoInteractions(appUserRepository);
        verify(userIdentityRepository, never()).save(any());
    }

    @Test
    void findOrCreate_doesNotResolveEmail_whenIdentityAlreadyExists() {
        // GIVEN a user that has logged in before
        AppUser existing = new AppUser("test@example.com");
        when(userIdentityRepository.findByProviderAndProviderUserId("github", "42"))
                .thenReturn(Optional.of(new UserIdentity(existing, "github", "42")));

        // WHEN the same GitHub account logs in again, and resolving the email would fail
        AppUser appUser = appUserService.findOrCreate("github", "42", () -> {
            throw new IllegalStateException("email must not be resolved for a known user");
        });

        // THEN the existing user is returned without touching the email supplier
        assertThat(appUser).isSameAs(existing);
    }
}
