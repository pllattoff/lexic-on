package com.lexicon.backend.service;

import com.lexicon.backend.dto.ProcessedText;
import com.lexicon.backend.dto.TextToken;
import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.VocabularyStatus;
import com.lexicon.backend.exception.WordEntryNotFoundException;
import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.model.UserVocabulary;
import com.lexicon.backend.model.WordEntry;
import com.lexicon.backend.repository.AppUserRepository;
import com.lexicon.backend.repository.UserVocabularyRepository;
import com.lexicon.backend.repository.WordEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VocabularyServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private WordEntryRepository wordEntryRepository;

    @Mock
    private UserVocabularyRepository userVocabularyRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private VocabularyService vocabularyService;

    @Test
    void saveStatus_createsVocabularyRow_whenWordIsSavedForTheFirstTime() {
        // GIVEN a processed word that the user has not tracked yet
        WordEntry hello = givenStoredWordEntry("hello");
        AppUser user = new AppUser("user@example.com");
        when(appUserRepository.getReferenceById(USER_ID)).thenReturn(user);

        // WHEN
        VocabularyStatus status = vocabularyService.saveStatus(
                USER_ID, "hello", SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.UNKNOWN);

        // THEN a new row links this user, word and target language with the status
        assertThat(status).isEqualTo(VocabularyStatus.UNKNOWN);
        ArgumentCaptor<UserVocabulary> savedRow = ArgumentCaptor.forClass(UserVocabulary.class);
        verify(userVocabularyRepository).save(savedRow.capture());
        assertThat(savedRow.getValue().getAppUser()).isSameAs(user);
        assertThat(savedRow.getValue().getWordEntry()).isSameAs(hello);
        assertThat(savedRow.getValue().getTargetLanguage()).isEqualTo(TargetLanguage.DE);
        assertThat(savedRow.getValue().getStatus()).isEqualTo(VocabularyStatus.UNKNOWN);
    }

    @Test
    void saveStatus_changesStatusOfExistingRow_whenWordIsAlreadyTracked() {
        // GIVEN a word already tracked as UNKNOWN
        WordEntry hello = givenStoredWordEntry("hello");
        UserVocabulary tracked = new UserVocabulary(
                new AppUser("user@example.com"), hello, TargetLanguage.DE, VocabularyStatus.UNKNOWN);
        when(userVocabularyRepository.findByAppUserIdAndWordEntryAndTargetLanguage(USER_ID, hello, TargetLanguage.DE))
                .thenReturn(Optional.of(tracked));

        // WHEN
        VocabularyStatus status = vocabularyService.saveStatus(
                USER_ID, "hello", SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.KNOWN);

        // THEN the same row gets the new status, and no new user reference is needed
        assertThat(status).isEqualTo(VocabularyStatus.KNOWN);
        assertThat(tracked.getStatus()).isEqualTo(VocabularyStatus.KNOWN);
        verify(userVocabularyRepository).save(tracked);
        verifyNoInteractions(appUserRepository);
    }

    @Test
    void saveStatus_throwsWordEntryNotFoundException_whenLemmaWasNeverProcessed() {
        // WHEN saving a status for a lemma that has no word entry (the repository mock finds nothing)
        assertThatThrownBy(() -> vocabularyService.saveStatus(
                USER_ID, "unknown-lemma", SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.UNKNOWN))
                // THEN nothing is stored
                .isInstanceOf(WordEntryNotFoundException.class);
        verifyNoInteractions(userVocabularyRepository);
    }

    @Test
    void assignStatusToLemmas_savesRowsOnlyForUntrackedWords_whenSomeWordsAreAlreadyTracked() {
        // GIVEN "hello" already tracked as REVIEW, "world" never tracked and "missing" without a word entry
        WordEntry hello = new WordEntry(SourceLanguage.EN, "hello");
        WordEntry world = new WordEntry(SourceLanguage.EN, "world");
        List<String> lemmas = List.of("hello", "world", "missing");
        when(wordEntryRepository.findByLemmaInAndLanguage(lemmas, SourceLanguage.EN))
                .thenReturn(List.of(hello, world));
        when(userVocabularyRepository.findByAppUserIdAndTargetLanguageAndWordEntryIn(
                eq(USER_ID), eq(TargetLanguage.DE), anyCollection()))
                .thenReturn(List.of(new UserVocabulary(
                        new AppUser("user@example.com"), hello, TargetLanguage.DE, VocabularyStatus.REVIEW)));

        // WHEN
        List<String> addedLemmas = vocabularyService.assignStatusToLemmas(
                USER_ID, lemmas, SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.KNOWN);

        // THEN only "world" is saved with the new status and returned
        assertThat(addedLemmas).containsExactly("world");
        ArgumentCaptor<List<UserVocabulary>> savedRows = ArgumentCaptor.captor();
        verify(userVocabularyRepository).saveAll(savedRows.capture());
        assertThat(savedRows.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getWordEntry()).isSameAs(world);
            assertThat(row.getTargetLanguage()).isEqualTo(TargetLanguage.DE);
            assertThat(row.getStatus()).isEqualTo(VocabularyStatus.KNOWN);
        });
    }

    @Test
    void assignStatusToLemmas_returnsEmptyList_whenNoLemmaHasAWordEntry() {
        // GIVEN lemmas that were never processed (the repository mock finds no word entries)
        List<String> lemmas = List.of("missing");

        // WHEN
        List<String> addedLemmas = vocabularyService.assignStatusToLemmas(
                USER_ID, lemmas, SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.UNKNOWN);

        // THEN nothing is added and the vocabulary is not even queried
        assertThat(addedLemmas).isEmpty();
        verifyNoInteractions(userVocabularyRepository, appUserRepository);
    }

    @Test
    void deleteStatus_deletesVocabularyRow_whenWordHasAnEntry() {
        // GIVEN a processed word
        WordEntry hello = givenStoredWordEntry("hello");

        // WHEN
        vocabularyService.deleteStatus(USER_ID, "hello", SourceLanguage.EN, TargetLanguage.DE);

        // THEN the row of this user, word and target language is deleted
        verify(userVocabularyRepository)
                .deleteByAppUserIdAndWordEntryAndTargetLanguage(USER_ID, hello, TargetLanguage.DE);
    }

    @Test
    void deleteStatus_throwsWordEntryNotFoundException_whenLemmaWasNeverProcessed() {
        // WHEN deleting a status for a lemma that has no word entry (the repository mock finds nothing)
        assertThatThrownBy(() -> vocabularyService.deleteStatus(
                USER_ID, "unknown-lemma", SourceLanguage.EN, TargetLanguage.DE))
        // THEN nothing is deleted
                .isInstanceOf(WordEntryNotFoundException.class);
        verifyNoInteractions(userVocabularyRepository);
    }

    @Test
    void attachStatuses_setsStatusOnEveryTokenOfTheLemma_andNullOnUntrackedWords() {
        // GIVEN "hello" tracked as REVIEW, "world" never clicked, "hello" occurring twice in the text
        WordEntry hello = new WordEntry(SourceLanguage.EN, "hello");
        WordEntry world = new WordEntry(SourceLanguage.EN, "world");
        when(wordEntryRepository.findByLemmaInAndLanguage(Set.of("hello", "world"), SourceLanguage.EN))
                .thenReturn(List.of(hello, world));
        when(userVocabularyRepository.findByAppUserIdAndTargetLanguageAndWordEntryIn(
                eq(USER_ID), eq(TargetLanguage.DE), anyCollection()))
                .thenReturn(List.of(new UserVocabulary(
                        new AppUser("user@example.com"), hello, TargetLanguage.DE, VocabularyStatus.REVIEW)));
        ProcessedText processedText = new ProcessedText("hello world hello", List.of(
                new TextToken(0, 5, "hello", "hallo", null),
                new TextToken(6, 11, "world", "Welt", null),
                new TextToken(12, 17, "hello", "hallo", null)));

        // WHEN
        ProcessedText result = vocabularyService.attachStatuses(
                processedText, SourceLanguage.EN, TargetLanguage.DE, USER_ID);

        // THEN both "hello" tokens get the status, the untracked word stays null
        assertThat(result.tokens())
                .extracting(TextToken::status)
                .containsExactly(VocabularyStatus.REVIEW, null, VocabularyStatus.REVIEW);
    }

    @Test
    void attachStatuses_doesNotQueryVocabulary_whenNoWordEntryExists() {
        // GIVEN a word without a word entry (the repository mock finds none)
        ProcessedText processedText = new ProcessedText("Hello", List.of(new TextToken(0, 5, "hello", "hallo", null)));

        // WHEN
        ProcessedText result = vocabularyService.attachStatuses(
                processedText, SourceLanguage.EN, TargetLanguage.DE, USER_ID);

        // THEN the word stays untracked and the user's vocabulary is not queried
        assertThat(result.tokens()).extracting(TextToken::status).containsOnlyNulls();
        verifyNoInteractions(userVocabularyRepository);
    }

    @Test
    void attachStatuses_doesNotQueryDatabase_whenTextHasNoTokens() {
        // GIVEN
        ProcessedText processedText = new ProcessedText("   ", List.of());

        // WHEN
        ProcessedText result = vocabularyService.attachStatuses(
                processedText, SourceLanguage.EN, TargetLanguage.DE, USER_ID);

        // THEN
        assertThat(result.tokens()).isEmpty();
        verifyNoInteractions(wordEntryRepository, userVocabularyRepository);
    }

    @Test
    void attachStatuses_keepsTextAndTokenData_whenStatusesAreAttached() {
        // GIVEN
        WordEntry hello = new WordEntry(SourceLanguage.EN, "hello");
        when(wordEntryRepository.findByLemmaInAndLanguage(Set.of("hello"), SourceLanguage.EN))
                .thenReturn(List.of(hello));
        when(userVocabularyRepository.findByAppUserIdAndTargetLanguageAndWordEntryIn(
                eq(USER_ID), eq(TargetLanguage.DE), anyCollection()))
                .thenReturn(List.of(new UserVocabulary(
                        new AppUser("user@example.com"), hello, TargetLanguage.DE, VocabularyStatus.UNKNOWN)));
        ProcessedText processedText = new ProcessedText("Hello", List.of(new TextToken(0, 5, "hello", "hallo", null)));

        // WHEN
        ProcessedText result = vocabularyService.attachStatuses(
                processedText, SourceLanguage.EN, TargetLanguage.DE, USER_ID);

        // THEN only the status is added
        assertThat(result.text()).isEqualTo("Hello");
        assertThat(result.tokens()).containsExactly(new TextToken(0, 5, "hello", "hallo", VocabularyStatus.UNKNOWN));
    }

    private WordEntry givenStoredWordEntry(String lemma) {
        WordEntry wordEntry = new WordEntry(SourceLanguage.EN, lemma);
        when(wordEntryRepository.findByLemmaAndLanguage(lemma, SourceLanguage.EN)).thenReturn(Optional.of(wordEntry));
        return wordEntry;
    }
}