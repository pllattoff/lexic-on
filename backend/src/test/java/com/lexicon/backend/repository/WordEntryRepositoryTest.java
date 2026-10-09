package com.lexicon.backend.repository;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.model.WordEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Integration tests for repository queries, entity mappings, and database constraints.
// Flyway migrations are applied before the tests.
// @DataJpaTest rolls back each test transaction.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WordEntryRepositoryTest {

    @Autowired
    private WordEntryRepository wordEntryRepository;

    @Test
    void findByLemmaInAndLanguage_returnsOnlyRequestedLemmas_whenOtherEntriesExist() {
        // GIVEN
        wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "hello"));
        wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "world"));
        wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "house"));

        // WHEN
        List<WordEntry> wordEntries = wordEntryRepository
                .findByLemmaInAndLanguage(List.of("hello", "world", "unknown"), SourceLanguage.EN);

        // THEN
        assertThat(wordEntries).extracting(WordEntry::getLemma).containsExactlyInAnyOrder("hello", "world");
    }

    @Test
    void save_throwsException_whenLanguageAndLemmaAlreadyExist() {
        // GIVEN
        wordEntryRepository.saveAndFlush(new WordEntry(SourceLanguage.EN, "hello"));
        WordEntry duplicate = new WordEntry(SourceLanguage.EN, "hello");

        // WHEN saving another word entry with the same language and lemma
        assertThatThrownBy(() -> wordEntryRepository.saveAndFlush(duplicate))
        // THEN the unique constraint rejects it
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByLemmaAndLanguage_returnsWordEntry_whenLemmaIsStored() {
        // GIVEN two stored word entries
        wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "hello"));
        wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "world"));

        // WHEN
        Optional<WordEntry> wordEntry = wordEntryRepository.findByLemmaAndLanguage("hello", SourceLanguage.EN);

        // THEN only the requested lemma is returned
        assertThat(wordEntry).get().extracting(WordEntry::getLemma).isEqualTo("hello");
    }

    @Test
    void findByLemmaAndLanguage_returnsEmpty_whenLemmaIsNotStored() {
        // WHEN
        Optional<WordEntry> wordEntry = wordEntryRepository.findByLemmaAndLanguage("unknown", SourceLanguage.EN);

        // THEN
        assertThat(wordEntry).isEmpty();
    }
}
