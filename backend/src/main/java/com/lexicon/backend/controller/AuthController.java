package com.lexicon.backend.controller;

import com.lexicon.backend.security.AppOAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    record MeResponse(String login, String email, String avatarUrl) {}

    // AppOAuth2User: Custom user combining OAuth2 provider attributes with application-specific user data
    @GetMapping("/me")
    public MeResponse getMe(@AuthenticationPrincipal AppOAuth2User user) {
        return new MeResponse(
                user.getAttribute("login"),
                user.getEmail(),
                user.getAttribute("avatar_url"));
    }

    @GetMapping("/csrf")
    public CsrfToken getCsrfToken(CsrfToken csrfToken) {
        return csrfToken;
    }

}