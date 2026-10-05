package com.lexicon.backend.service;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.model.Translation;
import com.lexicon.backend.model.WordEntry;
import com.lexicon.backend.repository.TranslationRepository;
import com.lexicon.backend.repository.WordEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

// @DataJpaTest rolls back every test, so no data is left in the shared in-memory H2
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TranslationPersistenceService.class)
class TranslationPersistenceServiceTest {

    @Autowired
    private TranslationPersistenceService translationPersistenceService;

    @Autowired
    private WordEntryRepository wordEntryRepository;

    @Autowired
    private TranslationRepository translationRepository;

    @Test
    void saveTranslations_createsWordEntriesAndTranslations_whenLemmasAreNew() {
        // GIVEN
        Map<String, String> translationsByLemma = Map.of("hello", "hallo", "world", "Welt");

        // WHEN
        translationPersistenceService.saveTranslations(translationsByLemma, SourceLanguage.EN, TargetLanguage.DE);

        // THEN each lemma gets its own word entry with its translation
        List<WordEntry> wordEntries = wordEntryRepository
                .findByLemmaInAndLanguage(translationsByLemma.keySet(), SourceLanguage.EN);
        assertThat(wordEntries).extracting(WordEntry::getLemma).containsExactlyInAnyOrder("hello", "world");

        List<Translation> translations = translationRepository
                .findByWordEntryInAndTargetLanguage(wordEntries, TargetLanguage.DE);
        assertThat(translations)
                .extracting(t -> t.getWordEntry().getLemma(), Translation::getTranslation)
                .containsExactlyInAnyOrder(
                        tuple("hello", "hallo"),
                        tuple("world", "Welt"));
    }

    @Test
    void saveTranslations_reusesWordEntry_whenItAlreadyExists() {
        // GIVEN a word entry that already exists, but has no translation yet
        WordEntry existing = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "hello"));

        // WHEN
        translationPersistenceService.saveTranslations(
                Map.of("hello", "hallo", "world", "Welt"), SourceLanguage.EN, TargetLanguage.DE);

        // THEN no duplicate word entry is created, and the existing one gets the translation
        List<WordEntry> wordEntries = wordEntryRepository.findByLemmaInAndLanguage(List.of("hello", "world"), SourceLanguage.EN);
        assertThat(wordEntries).hasSize(2);
        assertThat(wordEntries).extracting(WordEntry::getId).contains(existing.getId());

        List<Translation> helloTranslations = translationRepository
                .findByWordEntryInAndTargetLanguage(List.of(existing), TargetLanguage.DE);
        assertThat(helloTranslations).extracting(Translation::getTranslation).containsExactly("hallo");
    }

    @Test
    void saveTranslations_doesNotCreateDuplicateTranslation_whenTranslationAlreadyExists() {
        // GIVEN a word entry that already has a translation
        WordEntry existing = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "hello"));
        translationRepository.save(new Translation(existing, TargetLanguage.DE, "hallo"));

        // WHEN saving a translation for the same lemma and target language again
        translationPersistenceService.saveTranslations(
                Map.of("hello", "hallo"), SourceLanguage.EN, TargetLanguage.DE);

        // THEN the existing translation stays the only one
        List<Translation> translations = translationRepository
                .findByWordEntryInAndTargetLanguage(List.of(existing), TargetLanguage.DE);
        assertThat(translations).extracting(Translation::getTranslation).containsExactly("hallo");
    }
}
