package com.lexicon.backend.security;

import com.lexicon.backend.exception.VerifiedEmailNotFoundException;
import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserResolverTest {

    @Mock
    private AppUserService appUserService;

    @InjectMocks
    private AppUserResolver appUserResolver;

    @Test
    void resolve_returnsAppUser_whenServiceResolvesUser() {
        // GIVEN
        AppUser appUser = new AppUser("test@example.com");
        Supplier<Optional<String>> verifiedEmailSupplier = () -> Optional.of("test@example.com");
        when(appUserService.findOrCreate("google", "1092", verifiedEmailSupplier)).thenReturn(appUser);

        // WHEN
        AppUser resolved = appUserResolver.resolve("google", "1092", verifiedEmailSupplier);

        // THEN
        assertThat(resolved).isSameAs(appUser);
    }

    @Test
    void resolve_throwsOAuth2AuthenticationException_whenServiceFindsNoVerifiedEmail() {
        // GIVEN the service rejects the account because it has no verified email
        VerifiedEmailNotFoundException cause = new VerifiedEmailNotFoundException("no verified email");
        Supplier<Optional<String>> noVerifiedEmail = Optional::empty;
        when(appUserService.findOrCreate("google", "1092", noVerifiedEmail)).thenThrow(cause);

        // WHEN
        assertThatThrownBy(() -> appUserResolver.resolve("google", "1092", noVerifiedEmail))
        // THEN the login is rejected as an authentication failure
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception -> {
                    assertThat(exception.getError().getErrorCode()).isEqualTo("verified_email_not_found");
                    assertThat(exception.getCause()).isSameAs(cause);
                });
    }
}
