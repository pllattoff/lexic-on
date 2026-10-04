package com.lexicon.backend.service;

import com.lexicon.backend.dto.ProcessedText;
import com.lexicon.backend.dto.TextToken;
import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.exception.TranslationException;
import com.lexicon.backend.model.Translation;
import com.lexicon.backend.model.WordEntry;
import com.lexicon.backend.repository.TranslationRepository;
import com.lexicon.backend.repository.WordEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private WordEntryRepository wordEntryRepository;

    @Mock
    private TranslationRepository translationRepository;

    @Mock
    private AzureTranslatorClient azureTranslatorClient;

    @Mock
    private TranslationPersistenceService translationPersistenceService;

    @InjectMocks
    private TranslationService translationService;

    @Test
    void attachTranslations_returnsStoredTranslations_withoutCallingAzure_whenAllTranslationsExist() {
        // GIVEN both lemmas already have stored translations
        ProcessedText processedText = new ProcessedText("Hello world", List.of(
                new TextToken(0, 5, "hello", null),
                new TextToken(6, 11, "world", null)));
        givenStoredTranslations(Map.of("hello", "hallo", "world", "Welt"));

        // WHEN
        ProcessedText result = translationService.attachTranslations(processedText, SourceLanguage.EN, TargetLanguage.DE);

        // THEN the stored translations are attached and Azure is never called
        assertThat(result.tokens())
                .extracting(TextToken::translation)
                .containsExactly("hallo", "Welt");
        verifyNoInteractions(azureTranslatorClient, translationPersistenceService);
    }

    @Test
    void attachTranslations_translatesOnlyMissingLemmas_whenSomeTranslationsExist() {
        // GIVEN "hello" is stored, "world" is not
        ProcessedText processedText = new ProcessedText("Hello world", List.of(
                new TextToken(0, 5, "hello", null),
                new TextToken(6, 11, "world", null)));
        givenStoredTranslations(Map.of("hello", "hallo"));
        when(azureTranslatorClient.translate(List.of("world"), SourceLanguage.EN, TargetLanguage.DE))
                .thenReturn(List.of("Welt"));

        // WHEN
        ProcessedText result = translationService.attachTranslations(processedText, SourceLanguage.EN, TargetLanguage.DE);

        // THEN stored and freshly fetched translations are both attached
        assertThat(result.tokens())
                .extracting(TextToken::translation)
                .containsExactly("hallo", "Welt");
    }

    @Test
    void attachTranslations_savesOnlyNewTranslations_whenSomeTranslationsExist() {
        // GIVEN "hello" is stored, "world" is not
        ProcessedText processedText = new ProcessedText("Hello world", List.of(
                new TextToken(0, 5, "hello", null),
                new TextToken(6, 11, "world", null)));
        givenStoredTranslations(Map.of("hello", "hallo"));
        when(azureTranslatorClient.translate(List.of("world"), SourceLanguage.EN, TargetLanguage.DE))
                .thenReturn(List.of("Welt"));

        // WHEN
        translationService.attachTranslations(processedText, SourceLanguage.EN, TargetLanguage.DE);

        // THEN only the newly fetched translation is persisted
        verify(translationPersistenceService)
                .saveTranslations(Map.of("world", "Welt"), SourceLanguage.EN, TargetLanguage.DE);
    }

    @Test
    void attachTranslations_requestsEachLemmaOnce_whenLemmaAppearsMultipleTimes() {
        // GIVEN a text where the lemma "go" occurs twice, in different word forms
        ProcessedText processedText = new ProcessedText("go went home", List.of(
                new TextToken(0, 2, "go", null),
                new TextToken(3, 7, "go", null),
                new TextToken(8, 12, "home", null)));
        givenStoredTranslations(Map.of());
        when(azureTranslatorClient.translate(List.of("go", "home"), SourceLanguage.EN, TargetLanguage.DE))
                .thenReturn(List.of("gehen", "Heim"));

        // WHEN
        ProcessedText result = translationService.attachTranslations(processedText, SourceLanguage.EN, TargetLanguage.DE);

        // THEN Azure gets each lemma once, in order of first appearance, and every token is translated
        verify(azureTranslatorClient).translate(List.of("go", "home"), SourceLanguage.EN, TargetLanguage.DE);
        assertThat(result.tokens())
                .extracting(TextToken::translation)
                .containsExactly("gehen", "gehen", "Heim");
    }

    @Test
    void attachTranslations_keepsTextAndTokenPositions_whenTranslationsAreAttached() {
        // GIVEN
        ProcessedText processedText = new ProcessedText("Hello world", List.of(
                new TextToken(0, 5, "hello", null),
                new TextToken(6, 11, "world", null)));
        givenStoredTranslations(Map.of("hello", "hallo", "world", "Welt"));

        // WHEN
        ProcessedText result = translationService.attachTranslations(processedText, SourceLanguage.EN, TargetLanguage.DE);

        // THEN only the translation is added, everything else is unchanged
        assertThat(result.text()).isEqualTo("Hello world");
        assertThat(result.tokens()).containsExactly(
                new TextToken(0, 5, "hello", "hallo"),
                new TextToken(6, 11, "world", "Welt"));
    }

    @Test
    void attachTranslations_doesNotCallAzure_whenTextHasNoTokens() {
        // GIVEN
        ProcessedText processedText = new ProcessedText("   ", List.of());
        givenStoredTranslations(Map.of());

        // WHEN
        ProcessedText result = translationService.attachTranslations(processedText, SourceLanguage.EN, TargetLanguage.DE);

        // THEN
        assertThat(result.tokens()).isEmpty();
        verifyNoInteractions(azureTranslatorClient, translationPersistenceService);
    }

    @Test
    void attachTranslations_throwsAndSavesNothing_whenAzureFails() {
        // GIVEN no stored translations and Azure failing
        ProcessedText processedText = new ProcessedText("Hello", List.of(new TextToken(0, 5, "hello", null)));
        givenStoredTranslations(Map.of());
        when(azureTranslatorClient.translate(anyList(), any(), any()))
                .thenThrow(new TranslationException("Azure Translator request failed"));

        // WHEN
        assertThatThrownBy(() -> translationService.attachTranslations(processedText, SourceLanguage.EN, TargetLanguage.DE))
        // THEN the exception is propagated and nothing is persisted
                .isInstanceOf(TranslationException.class);
        verify(translationPersistenceService, never()).saveTranslations(any(), any(), any());
    }

    // Stubs the repositories so that exactly the given translations (lemma -> translation) are "stored"
    private void givenStoredTranslations(Map<String, String> storedTranslationsByLemma) {
        List<WordEntry> wordEntries = storedTranslationsByLemma.keySet().stream()
                .map(lemma -> new WordEntry(SourceLanguage.EN, lemma))
                .toList();
        List<Translation> translations = wordEntries.stream()
                .map(wordEntry -> new Translation(wordEntry, TargetLanguage.DE, storedTranslationsByLemma.get(wordEntry.getLemma())))
                .toList();

        when(wordEntryRepository.findByLemmaInAndLanguage(any(), any())).thenReturn(wordEntries);
        when(translationRepository.findByWordEntryInAndTargetLanguage(any(), any())).thenReturn(translations);
    }
}
