package com.lexicon.backend.controller;

import tools.jackson.databind.ObjectMapper;
import com.lexicon.backend.dto.ProcessTextRequest;
import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.UserRole;
import com.lexicon.backend.enums.VocabularyStatus;
import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.model.Translation;
import com.lexicon.backend.model.UserVocabulary;
import com.lexicon.backend.model.WordEntry;
import com.lexicon.backend.repository.AppUserRepository;
import com.lexicon.backend.repository.TranslationRepository;
import com.lexicon.backend.repository.UserVocabularyRepository;
import com.lexicon.backend.repository.WordEntryRepository;
import com.lexicon.backend.security.AppOAuth2User;
import com.lexicon.backend.security.GithubApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
// Database changes made during the test run within a transaction that is rolled back afterwards
@Transactional
class TextControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer mockRestServiceServer;

    @Autowired
    private WordEntryRepository wordEntryRepository;

    @Autowired
    private TranslationRepository translationRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserVocabularyRepository userVocabularyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Not used here. Replacing it keeps Azure the only RestClient bound to MockRestServiceServer
    @MockitoBean
    private GithubApiClient githubApiClient;

    @Test
    void process_returnsBadRequest_whenTextIsBlank() throws Exception {
        // GIVEN
        String requestBody = objectMapper.writeValueAsString(new ProcessTextRequest(" ", SourceLanguage.EN, TargetLanguage.DE));

        // WHEN
        mockMvc.perform(post("/api/text/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN
                .andExpect(status().isBadRequest());
    }

    @Test
    void process_returnsForbidden_whenCsrfTokenIsMissing() throws Exception {
        // GIVEN
        String requestBody = objectMapper.writeValueAsString(new ProcessTextRequest("Hello world", SourceLanguage.EN, TargetLanguage.DE));

        // WHEN posting it without a CSRF token
        mockMvc.perform(post("/api/text/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN the response is 403 Forbidden
                .andExpect(status().isForbidden());
    }

    @Test
    void process_returnsProcessedText_whenCalledByGuest() throws Exception {
        // GIVEN Azure Translator answering for the two lemmas in order of appearance
        mockRestServiceServer
                .expect(requestTo("http://localhost/translate?api-version=3.0&from=en&to=de"))
                .andExpect(content().json("""
                        [{"text": "hello"}, {"text": "world"}]
                        """))
                .andRespond(withSuccess("""
                        [
                          {"translations": [{"text": "hallo", "to": "de"}]},
                          {"translations": [{"text": "Welt", "to": "de"}]}
                        ]
                        """, MediaType.APPLICATION_JSON));
        String requestBody = objectMapper.writeValueAsString(new ProcessTextRequest("Hello world", SourceLanguage.EN, TargetLanguage.DE));

        // WHEN posting it with a valid CSRF token and no authentication
        mockMvc.perform(post("/api/text/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN the response is 200 OK with the word tokens, their positions, lemmas, and translations
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Hello world"))
                .andExpect(jsonPath("$.tokens", hasSize(2)))
                .andExpect(jsonPath("$.tokens[0].start").value(0))
                .andExpect(jsonPath("$.tokens[0].end").value(5))
                .andExpect(jsonPath("$.tokens[0].lemma").value("hello"))
                .andExpect(jsonPath("$.tokens[0].translation").value("hallo"))
                .andExpect(jsonPath("$.tokens[1].start").value(6))
                .andExpect(jsonPath("$.tokens[1].end").value(11))
                .andExpect(jsonPath("$.tokens[1].lemma").value("world"))
                .andExpect(jsonPath("$.tokens[1].translation").value("Welt"))
                .andExpect(jsonPath("$.tokens[0].status").value(nullValue()))
                .andExpect(jsonPath("$.tokens[1].status").value(nullValue()));
        mockRestServiceServer.verify();
    }

    @Test
    void process_returnsVocabularyStatuses_whenCalledByLoggedInUser() throws Exception {
        // GIVEN stored translations, and a logged-in user who tracks "hello" but has never clicked "world"
        WordEntry hello = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "hello"));
        WordEntry world = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "world"));
        translationRepository.save(new Translation(hello, TargetLanguage.DE, "hallo"));
        translationRepository.save(new Translation(world, TargetLanguage.DE, "Welt"));
        AppUser appUser = appUserRepository.save(new AppUser("test@example.com"));
        userVocabularyRepository.save(new UserVocabulary(appUser, hello, TargetLanguage.DE, VocabularyStatus.REVIEW));
        AppOAuth2User loggedInUser = new AppOAuth2User(appUser.getId(), appUser.getEmail(), UserRole.USER, Map.of("id", 42));
        String requestBody = objectMapper.writeValueAsString(new ProcessTextRequest("Hello world", SourceLanguage.EN, TargetLanguage.DE));

        // WHEN
        mockMvc.perform(post("/api/text/process")
                        .with(oauth2Login().oauth2User(loggedInUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN the tracked word carries its status, the untracked one has none
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens[0].status").value("REVIEW"))
                .andExpect(jsonPath("$.tokens[1].status").value(nullValue()));
    }

    @Test
    void process_returnsStoredTranslations_withoutCallingAzure_whenTranslationsAlreadyExist() throws Exception {
        // GIVEN translations for both lemmas already stored in the database, and no Azure request expected
        WordEntry hello = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "hello"));
        WordEntry world = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "world"));
        translationRepository.save(new Translation(hello, TargetLanguage.DE, "hallo"));
        translationRepository.save(new Translation(world, TargetLanguage.DE, "Welt"));
        String requestBody = objectMapper.writeValueAsString(new ProcessTextRequest("Hello world", SourceLanguage.EN, TargetLanguage.DE));

        // WHEN
        mockMvc.perform(post("/api/text/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN the stored translations are returned (any Azure call would fail the test: no request is expected)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens", hasSize(2)))
                .andExpect(jsonPath("$.tokens[0].translation").value("hallo"))
                .andExpect(jsonPath("$.tokens[1].translation").value("Welt"));
        mockRestServiceServer.verify();
    }
}