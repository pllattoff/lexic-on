package com.lexicon.backend.controller;

import com.lexicon.backend.security.AppPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    record MeResponse(String name, String email, String avatarUrl) {}

    // AppPrincipal: the signed-in user, whichever provider (GitHub or Google) was used to sign in
    @GetMapping("/me")
    public MeResponse getMe(@AuthenticationPrincipal AppPrincipal user) {
        return new MeResponse(
                user.getDisplayName(),
                user.getEmail(),
                user.getAvatarUrl());
    }

    @GetMapping("/csrf")
    public CsrfToken getCsrfToken(CsrfToken csrfToken) {
        return csrfToken;
    }

}