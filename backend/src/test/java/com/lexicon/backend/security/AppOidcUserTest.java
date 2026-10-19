package com.lexicon.backend.security;

import com.lexicon.backend.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppOidcUserTest {

    @Test
    void getDisplayName_returnsGivenName_whenGoogleProvidesIt() {
        // GIVEN
        OidcUser googleUser = googleUser(Map.of("given_name", "John", "name", "John Doe"));

        // WHEN
        AppOidcUser user = new AppOidcUser(1L, "test@example.com", UserRole.USER, googleUser);

        // THEN
        assertThat(user.getDisplayName()).isEqualTo("John");
    }

    @Test
    void getDisplayName_returnsFullName_whenGivenNameIsMissing() {
        // GIVEN
        OidcUser googleUser = googleUser(Map.of("name", "John Doe"));

        // WHEN
        AppOidcUser user = new AppOidcUser(1L, "test@example.com", UserRole.USER, googleUser);

        // THEN
        assertThat(user.getDisplayName()).isEqualTo("John Doe");
    }

    @Test
    void getDisplayName_returnsEmailLocalPart_whenGoogleProvidesNoName() {
        // GIVEN
        OidcUser googleUser = googleUser(Map.of());

        // WHEN
        AppOidcUser user = new AppOidcUser(1L, "test@example.com", UserRole.USER, googleUser);

        // THEN
        assertThat(user.getDisplayName()).isEqualTo("test");
    }

    @Test
    void getAvatarUrl_returnsPicture_whenGoogleProvidesIt() {
        // GIVEN
        OidcUser googleUser = googleUser(Map.of("picture", "https://avatars.example.com/testuser.png"));

        // WHEN
        AppOidcUser user = new AppOidcUser(1L, "test@example.com", UserRole.USER, googleUser);

        // THEN
        assertThat(user.getAvatarUrl()).isEqualTo("https://avatars.example.com/testuser.png");
    }

    @Test
    void constructor_usesSubAsNameAndEmailOfAppUserAndGrantsRole_whenUserIsCreated() {
        // GIVEN a Google sign-in whose email claim differs from the email stored in AppUser
        OidcUser googleUser = googleUser(Map.of("email", "claim@example.com"));

        // WHEN
        AppOidcUser user = new AppOidcUser(1L, "stored@example.com", UserRole.USER, googleUser);

        // THEN the principal is identified by the Google ID, and the email is the one of AppUser
        assertThat(user.getName()).isEqualTo("1092");
        assertThat(user.getAppUserId()).isEqualTo(1L);
        assertThat(user.getEmail()).isEqualTo("stored@example.com");
        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    // A Google user with the given claims besides the required "sub"
    private OidcUser googleUser(Map<String, Object> claims) {
        Map<String, Object> allClaims = new HashMap<>(claims);
        allClaims.put("sub", "1092");
        OidcIdToken idToken = new OidcIdToken(
                "token-value", Instant.now(), Instant.now().plusSeconds(60), allClaims);

        return new DefaultOidcUser(List.of(), idToken);
    }
}
