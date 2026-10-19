package com.lexicon.backend.security;

import com.lexicon.backend.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

// Represents a user who signed in with GitHub in Spring Security:
// GitHub attributes combined with application-specific AppUser data.
// DefaultOAuth2User is Serializable, allowing the principal to be stored in the HTTP session.
@Getter
public class AppOAuth2User extends DefaultOAuth2User implements AppPrincipal {

    private final Long appUserId;
    private final String email;

    public AppOAuth2User(Long appUserId, String email, UserRole role, Map<String, Object> attributes) {
        // Use the stable GitHub user ID ("id") as the Spring Security principal name
        super(List.of(new SimpleGrantedAuthority("ROLE_" + role.name())), attributes, "id");
        this.appUserId = appUserId;
        this.email = email;
    }

    @Override
    public String getDisplayName() {
        return getAttribute("login");
    }

    @Override
    public String getAvatarUrl() {
        return getAttribute("avatar_url");
    }
}