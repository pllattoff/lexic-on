package com.lexicon.backend.service;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.model.Translation;
import com.lexicon.backend.model.WordEntry;
import com.lexicon.backend.repository.TranslationRepository;
import com.lexicon.backend.repository.WordEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationPersistenceServiceTest {

    @Mock
    private WordEntryRepository wordEntryRepository;

    @Mock
    private TranslationRepository translationRepository;

    @InjectMocks
    private TranslationPersistenceService translationPersistenceService;

    @Captor
    private ArgumentCaptor<List<WordEntry>> savedWordEntries;

    @Captor
    private ArgumentCaptor<List<Translation>> savedTranslations;

    @Test
    void saveTranslations_createsWordEntriesAndTranslations_whenLemmasAreNew() {
        // GIVEN
        Map<String, String> translationsByLemma = Map.of("hello", "hallo", "world", "Welt");

        // WHEN
        translationPersistenceService.saveTranslations(translationsByLemma, SourceLanguage.EN, TargetLanguage.DE);

        // THEN each lemma gets its own word entry with its translation
        verify(wordEntryRepository).saveAll(savedWordEntries.capture());
        assertThat(savedWordEntries.getValue())
                .extracting(WordEntry::getLemma)
                .containsExactlyInAnyOrder("hello", "world");

        verify(translationRepository).saveAll(savedTranslations.capture());
        assertThat(savedTranslations.getValue())
                .extracting(t -> t.getWordEntry().getLemma(), Translation::getTranslation)
                .containsExactlyInAnyOrder(
                        tuple("hello", "hallo"),
                        tuple("world", "Welt"));
    }

    @Test
    void saveTranslations_reusesWordEntry_whenItAlreadyExists() {
        // GIVEN a word entry that already exists, but has no translation yet
        WordEntry existing = givenStoredWordEntry(1L, "hello");

        // WHEN
        translationPersistenceService.saveTranslations(
                Map.of("hello", "hallo", "world", "Welt"), SourceLanguage.EN, TargetLanguage.DE);

        // THEN only the missing word entry is created
        verify(wordEntryRepository).saveAll(savedWordEntries.capture());
        assertThat(savedWordEntries.getValue()).extracting(WordEntry::getLemma).containsExactly("world");

        // AND the existing word entry itself gets its translation
        verify(translationRepository).saveAll(savedTranslations.capture());
        assertThat(savedTranslations.getValue())
                .filteredOn(t -> t.getTranslation().equals("hallo"))
                .singleElement()
                .satisfies(t -> assertThat(t.getWordEntry()).isSameAs(existing));
    }

    @Test
    void saveTranslations_doesNotCreateDuplicateTranslation_whenTranslationAlreadyExists() {
        // GIVEN a word entry that already has a translation
        WordEntry existing = givenStoredWordEntry(1L, "hello");
        when(translationRepository.findByWordEntryInAndTargetLanguage(anyCollection(), eq(TargetLanguage.DE)))
                .thenReturn(List.of(new Translation(existing, TargetLanguage.DE, "hallo")));

        // WHEN saving a translation for the same lemma and target language again
        translationPersistenceService.saveTranslations(
                Map.of("hello", "hallo"), SourceLanguage.EN, TargetLanguage.DE);

        // THEN neither a word entry nor a translation is created
        verify(wordEntryRepository).saveAll(savedWordEntries.capture());
        assertThat(savedWordEntries.getValue()).isEmpty();

        verify(translationRepository).saveAll(savedTranslations.capture());
        assertThat(savedTranslations.getValue()).isEmpty();
    }

    // The id is normally assigned by the database and the service deduplicates by it,
    // so the word entry is a mock with a stubbed id
    private WordEntry givenStoredWordEntry(Long id, String lemma) {
        WordEntry wordEntry = mock(WordEntry.class);
        when(wordEntry.getId()).thenReturn(id);
        when(wordEntry.getLemma()).thenReturn(lemma);
        when(wordEntryRepository.findByLemmaInAndLanguage(anyCollection(), eq(SourceLanguage.EN)))
                .thenReturn(List.of(wordEntry));
        return wordEntry;
    }
}
