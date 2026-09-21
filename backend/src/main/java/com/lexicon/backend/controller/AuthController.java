package com.lexicon.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    record MeResponse(String login, String email, String avatarUrl) {}

    @GetMapping("/me")
    public MeResponse getMe(@AuthenticationPrincipal OAuth2User user) {
        return new MeResponse(
                user.getAttribute("login"),
                user.getAttribute("email"),
                user.getAttribute("avatar_url"));
    }

    @GetMapping("/csrf")
    public CsrfToken getCsrfToken(CsrfToken csrfToken) {
        return csrfToken;
    }

}
