package com.lexicon.backend.controller;

import com.lexicon.backend.dto.DeleteVocabularyStatusRequest;
import com.lexicon.backend.dto.SaveVocabularyStatusRequest;
import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.UserRole;
import com.lexicon.backend.enums.VocabularyStatus;
import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.model.UserVocabulary;
import com.lexicon.backend.model.WordEntry;
import com.lexicon.backend.repository.AppUserRepository;
import com.lexicon.backend.repository.UserVocabularyRepository;
import com.lexicon.backend.repository.WordEntryRepository;
import com.lexicon.backend.security.AppOAuth2User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
// Database changes made during the test run within a transaction that is rolled back afterwards
@Transactional
class VocabularyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private WordEntryRepository wordEntryRepository;

    @Autowired
    private UserVocabularyRepository userVocabularyRepository;

    private AppOAuth2User loggedInUser;

    @BeforeEach
    void setUp() {
        AppUser appUser = appUserRepository.save(new AppUser("test@example.com"));
        loggedInUser = new AppOAuth2User(appUser.getId(), appUser.getEmail(), UserRole.USER, Map.of("id", 42));
        wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "hello"));
    }

    @Test
    void saveStatus_returnsSavedStatus_whenUserIsLoggedIn() throws Exception {
        // GIVEN
        String requestBody = saveRequestBody("hello", VocabularyStatus.UNKNOWN);

        // WHEN
        mockMvc.perform(put("/api/vocabulary")
                        .with(oauth2Login().oauth2User(loggedInUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN the new status is returned and stored
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNKNOWN"));
        assertThat(userVocabularyRepository.findAll())
                .extracting(UserVocabulary::getStatus)
                .containsExactly(VocabularyStatus.UNKNOWN);
    }

    @Test
    void saveStatus_returnsUnauthorized_whenUserIsGuest() throws Exception {
        // GIVEN
        String requestBody = saveRequestBody("hello", VocabularyStatus.UNKNOWN);

        // WHEN
        mockMvc.perform(put("/api/vocabulary")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN
                .andExpect(status().isUnauthorized());
    }

    @Test
    void saveStatus_returnsForbidden_whenCsrfTokenIsMissing() throws Exception {
        // GIVEN
        String requestBody = saveRequestBody("hello", VocabularyStatus.UNKNOWN);

        // WHEN a logged-in user sends the request without a CSRF token
        mockMvc.perform(put("/api/vocabulary")
                        .with(oauth2Login().oauth2User(loggedInUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN
                .andExpect(status().isForbidden());
    }

    @Test
    void saveStatus_returnsNotFound_whenLemmaWasNeverProcessed() throws Exception {
        // GIVEN
        String requestBody = saveRequestBody("never-processed", VocabularyStatus.UNKNOWN);

        // WHEN
        mockMvc.perform(put("/api/vocabulary")
                        .with(oauth2Login().oauth2User(loggedInUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN
                .andExpect(status().isNotFound());
    }

    @Test
    void saveStatus_returnsBadRequest_whenLemmaIsBlank() throws Exception {
        // GIVEN
        String requestBody = saveRequestBody(" ", VocabularyStatus.UNKNOWN);

        // WHEN
        mockMvc.perform(put("/api/vocabulary")
                        .with(oauth2Login().oauth2User(loggedInUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteStatus_removesWordFromVocabulary_whenUserIsLoggedIn() throws Exception {
        // GIVEN a tracked word
        mockMvc.perform(put("/api/vocabulary")
                .with(oauth2Login().oauth2User(loggedInUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveRequestBody("hello", VocabularyStatus.REVIEW)));
        String requestBody = objectMapper.writeValueAsString(
                new DeleteVocabularyStatusRequest("hello", SourceLanguage.EN, TargetLanguage.DE));

        // WHEN
        mockMvc.perform(delete("/api/vocabulary")
                        .with(oauth2Login().oauth2User(loggedInUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN the response is 204 No Content and the row is gone
                .andExpect(status().isNoContent());
        assertThat(userVocabularyRepository.findAll()).isEmpty();
    }

    @Test
    void deleteStatus_returnsUnauthorized_whenUserIsGuest() throws Exception {
        // GIVEN
        String requestBody = objectMapper.writeValueAsString(
                new DeleteVocabularyStatusRequest("hello", SourceLanguage.EN, TargetLanguage.DE));

        // WHEN
        mockMvc.perform(delete("/api/vocabulary")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN
                .andExpect(status().isUnauthorized());
    }

    private String saveRequestBody(String lemma, VocabularyStatus vocabularyStatus) {
        return objectMapper.writeValueAsString(
                new SaveVocabularyStatusRequest(lemma, SourceLanguage.EN, TargetLanguage.DE, vocabularyStatus));
    }
}