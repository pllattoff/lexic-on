package com.lexicon.backend.security;

import com.lexicon.backend.enums.UserRole;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;

// Represents a user who signed in with Google in Spring Security:
// the OpenID Connect claims combined with application-specific AppUser data.
// DefaultOidcUser is Serializable, allowing the principal to be stored in the HTTP session.
public class AppOidcUser extends DefaultOidcUser implements AppPrincipal {

    private final Long appUserId;
    private final String email;

    public AppOidcUser(Long appUserId, String email, UserRole role, OidcUser googleUser) {
        // Use the stable Google user ID ("sub") as the Spring Security principal name
        super(List.of(new SimpleGrantedAuthority("ROLE_" + role.name())),
                googleUser.getIdToken(), googleUser.getUserInfo(), "sub");
        this.appUserId = appUserId;
        this.email = email;
    }

    @Override
    public Long getAppUserId() {
        return appUserId;
    }

    // The email of our own AppUser; OidcUser.getEmail() would return the email claim of this sign-in instead
    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getDisplayName() {
        if (getGivenName() != null) {
            return getGivenName();
        }
        if (getFullName() != null) {
            return getFullName();
        }
        return email.substring(0, email.indexOf('@'));
    }

    @Override
    public String getAvatarUrl() {
        return getPicture();
    }
}
