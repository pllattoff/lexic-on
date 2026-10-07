package com.lexicon.backend.controller;

import com.lexicon.backend.enums.UserRole;
import com.lexicon.backend.security.AppOAuth2User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMe_returnsCurrentUser_whenLoggedIn() throws Exception {
        // GIVEN a logged-in user
        AppOAuth2User user = new AppOAuth2User(1L, "test@example.com", UserRole.USER, Map.of(
                "id", 42,
                "login", "testuser",
                "avatar_url", "https://avatars.example.com/testuser.png"));

        // WHEN
        mockMvc.perform(get("/api/auth/me").with(oauth2Login().oauth2User(user)))
        // THEN the login and avatar come from GitHub, the email from our own AppUser
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.avatarUrl").value("https://avatars.example.com/testuser.png"));
    }

    @Test
    void getMe_returnsUnauthorized_whenGuest() throws Exception {
        // WHEN
        mockMvc.perform(get("/api/auth/me"))
        // THEN
                .andExpect(status().isUnauthorized());
    }
}