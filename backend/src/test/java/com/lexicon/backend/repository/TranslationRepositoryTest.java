package com.lexicon.backend.repository;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.model.Translation;
import com.lexicon.backend.model.WordEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Integration tests for repository queries, entity mappings, and database constraints.
// Flyway migrations are applied before the tests.
// @DataJpaTest rolls back each test transaction.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TranslationRepositoryTest {

    @Autowired
    private WordEntryRepository wordEntryRepository;

    @Autowired
    private TranslationRepository translationRepository;

    @Test
    void findByWordEntryInAndTargetLanguageOrderById_returnsOnlyMatchingTranslations_whenOthersExist() {
        // GIVEN
        WordEntry hello = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "hello"));
        WordEntry world = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "world"));
        translationRepository.save(new Translation(hello, TargetLanguage.DE, "hallo"));
        translationRepository.save(new Translation(hello, TargetLanguage.UK, "привіт"));
        translationRepository.save(new Translation(world, TargetLanguage.DE, "Welt"));

        // WHEN
        List<Translation> translations = translationRepository
                .findByWordEntryInAndTargetLanguageOrderById(List.of(hello), TargetLanguage.DE);

        // THEN
        assertThat(translations).extracting(Translation::getTranslation).containsExactly("hallo");
    }

    @Test
    void findByWordEntryInAndTargetLanguageOrderById_returnsTranslationsInInsertionOrder_whenWordEntryHasSeveral() {
        // GIVEN translations saved in an order that differs from alphabetical order
        WordEntry the = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "the"));
        translationRepository.save(new Translation(the, TargetLanguage.DE, "der"));
        translationRepository.save(new Translation(the, TargetLanguage.DE, "die"));
        translationRepository.save(new Translation(the, TargetLanguage.DE, "das"));

        // WHEN
        List<Translation> translations = translationRepository
                .findByWordEntryInAndTargetLanguageOrderById(List.of(the), TargetLanguage.DE);

        // THEN they come back ordered by id
        assertThat(translations).extracting(Translation::getTranslation).containsExactly("der", "die", "das");
    }

    @Test
    void save_storesBothTranslations_whenWordEntryHasDifferentTranslations() {
        // GIVEN
        WordEntry bank = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "bank"));
        translationRepository.saveAndFlush(new Translation(bank, TargetLanguage.DE, "Bank"));

        // WHEN adding a different German translation for the same word entry
        translationRepository.saveAndFlush(new Translation(bank, TargetLanguage.DE, "Ufer"));

        // THEN both translations are stored
        assertThat(translationRepository.findByWordEntryInAndTargetLanguageOrderById(List.of(bank), TargetLanguage.DE))
                .extracting(Translation::getTranslation).containsExactlyInAnyOrder("Bank", "Ufer");
    }

    @Test
    void save_throwsException_whenSameTranslationAlreadyExists() {
        // GIVEN
        WordEntry hello = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "hello"));
        translationRepository.saveAndFlush(new Translation(hello, TargetLanguage.DE, "hallo"));
        Translation duplicate = new Translation(hello, TargetLanguage.DE, "hallo");

        // WHEN saving the same translation for the same word entry and language again
        assertThatThrownBy(() -> translationRepository.saveAndFlush(duplicate))
        // THEN the unique constraint rejects it
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
