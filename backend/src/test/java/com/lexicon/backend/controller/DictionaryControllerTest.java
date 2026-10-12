package com.lexicon.backend.controller;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
// Database changes made during the test run within a transaction that is rolled back afterwards
@Transactional
class DictionaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private WordEntryRepository wordEntryRepository;

    @Autowired
    private TranslationRepository translationRepository;

    @Autowired
    private UserVocabularyRepository userVocabularyRepository;

    private AppOAuth2User loggedInUser;

    @BeforeEach
    void setUp() {
        AppUser appUser = appUserRepository.save(new AppUser("test@example.com"));
        loggedInUser = new AppOAuth2User(appUser.getId(), appUser.getEmail(), UserRole.USER, Map.of("id", 42));

        WordEntry hello = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "hello"));
        WordEntry world = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "world"));
        translationRepository.save(new Translation(hello, TargetLanguage.DE, "hallo"));
        translationRepository.save(new Translation(world, TargetLanguage.DE, "Welt"));
        userVocabularyRepository.save(new UserVocabulary(appUser, hello, TargetLanguage.DE, VocabularyStatus.UNKNOWN));
        userVocabularyRepository.save(new UserVocabulary(appUser, world, TargetLanguage.DE, VocabularyStatus.UNKNOWN));
    }

    @Test
    void getDictionaries_returnsPairsWithWordCounts_whenUserIsLoggedIn() throws Exception {
        // WHEN
        mockMvc.perform(get("/api/dictionaries")
                        .with(oauth2Login().oauth2User(loggedInUser)))
        // THEN the pair is returned with counts of all three statuses
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sourceLanguage").value("en"))
                .andExpect(jsonPath("$[0].targetLanguage").value("de"))
                .andExpect(jsonPath("$[0].wordCounts.UNKNOWN").value(2))
                .andExpect(jsonPath("$[0].wordCounts.REVIEW").value(0))
                .andExpect(jsonPath("$[0].wordCounts.KNOWN").value(0));
    }

    @Test
    void getDictionaries_returnsUnauthorized_whenUserIsGuest() throws Exception {
        // WHEN
        mockMvc.perform(get("/api/dictionaries"))
        // THEN
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getWords_returnsWordsWithTranslations_whenUserIsLoggedIn() throws Exception {
        // WHEN
        mockMvc.perform(get("/api/dictionaries/en/de/words")
                        .param("status", "UNKNOWN")
                        .with(oauth2Login().oauth2User(loggedInUser)))
        // THEN the newest word comes first
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].lemma").value("world"))
                .andExpect(jsonPath("$[0].translation").value("Welt"))
                .andExpect(jsonPath("$[1].lemma").value("hello"))
                .andExpect(jsonPath("$[1].translation").value("hallo"));
    }

    @Test
    void getWords_returnsEmptyList_whenNoWordHasThisStatus() throws Exception {
        // WHEN
        mockMvc.perform(get("/api/dictionaries/en/de/words")
                        .param("status", "KNOWN")
                        .with(oauth2Login().oauth2User(loggedInUser)))
        // THEN
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getWords_returnsBadRequest_whenLanguageCodeIsUnknown() throws Exception {
        // WHEN
        mockMvc.perform(get("/api/dictionaries/en/xx/words")
                        .param("status", "UNKNOWN")
                        .with(oauth2Login().oauth2User(loggedInUser)))
        // THEN
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWords_returnsUnauthorized_whenUserIsGuest() throws Exception {
        // WHEN
        mockMvc.perform(get("/api/dictionaries/en/de/words")
                        .param("status", "UNKNOWN"))
        // THEN
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteDictionary_removesAllWordsOfTheLanguagePair_whenUserIsLoggedIn() throws Exception {
        // GIVEN the two German words from the setup and one more word tracked for Ukrainian
        AppUser appUser = appUserRepository.findById(loggedInUser.getAppUserId()).orElseThrow();
        WordEntry hello = wordEntryRepository.findByLemmaAndLanguage("hello", SourceLanguage.EN).orElseThrow();
        userVocabularyRepository.save(new UserVocabulary(appUser, hello, TargetLanguage.UK, VocabularyStatus.KNOWN));

        // WHEN deleting the English → German dictionary
        mockMvc.perform(delete("/api/dictionaries/en/de")
                        .with(oauth2Login().oauth2User(loggedInUser))
                        .with(csrf()))
        // THEN the response is 204 No Content and only the Ukrainian row is left
                .andExpect(status().isNoContent());
        assertThat(userVocabularyRepository.findAll())
                .extracting(UserVocabulary::getTargetLanguage)
                .containsExactly(TargetLanguage.UK);
    }

    @Test
    void deleteDictionary_returnsUnauthorized_whenUserIsGuest() throws Exception {
        // WHEN
        mockMvc.perform(delete("/api/dictionaries/en/de")
                        .with(csrf()))
        // THEN
                .andExpect(status().isUnauthorized());
    }
}
