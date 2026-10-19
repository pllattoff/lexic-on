package com.lexicon.backend.security;

// The authenticated user as the rest of the application sees it, regardless of the sign-in provider.
// GitHub users are represented by AppOAuth2User and Google users by AppOidcUser.
public interface AppPrincipal {

    Long getAppUserId();

    String getEmail();

    // Named getDisplayName because OAuth2User.getName() already returns the provider's user ID
    String getDisplayName();

    String getAvatarUrl();
}
