package com.lexicon.backend.service;

import com.lexicon.backend.dto.DictionarySummary;
import com.lexicon.backend.dto.DictionaryWord;
import com.lexicon.backend.dto.LanguagePairStatusCount;
import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.VocabularyStatus;
import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.model.Translation;
import com.lexicon.backend.model.UserVocabulary;
import com.lexicon.backend.model.WordEntry;
import com.lexicon.backend.repository.TranslationRepository;
import com.lexicon.backend.repository.UserVocabularyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserVocabularyRepository userVocabularyRepository;

    @Mock
    private TranslationRepository translationRepository;

    @InjectMocks
    private DictionaryService dictionaryService;

    @Test
    void getDictionaries_returnsZeroForMissingStatuses_whenPairHasWordsOnlyInSomeStatuses() {
        // GIVEN a pair with words only in UNKNOWN and KNOWN
        when(userVocabularyRepository.countWordsByLanguagePairAndStatus(USER_ID)).thenReturn(List.of(
                new LanguagePairStatusCount(SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.UNKNOWN, 3),
                new LanguagePairStatusCount(SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.KNOWN, 5)));

        // WHEN
        List<DictionarySummary> dictionaries = dictionaryService.getDictionaries(USER_ID);

        // THEN the pair has all three statuses, the missing one is zero
        assertThat(dictionaries).hasSize(1);
        assertThat(dictionaries.getFirst().wordCounts()).containsExactlyInAnyOrderEntriesOf(Map.of(
                VocabularyStatus.UNKNOWN, 3L,
                VocabularyStatus.REVIEW, 0L,
                VocabularyStatus.KNOWN, 5L));
    }

    @Test
    void getDictionaries_returnsOneSummaryPerPairInQueryOrder_whenUserHasSeveralPairs() {
        // GIVEN counts of two pairs, as the query returns them (sorted by language)
        when(userVocabularyRepository.countWordsByLanguagePairAndStatus(USER_ID)).thenReturn(List.of(
                new LanguagePairStatusCount(SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.KNOWN, 2),
                new LanguagePairStatusCount(SourceLanguage.EN, TargetLanguage.UK, VocabularyStatus.REVIEW, 4),
                new LanguagePairStatusCount(SourceLanguage.EN, TargetLanguage.UK, VocabularyStatus.UNKNOWN, 1)));

        // WHEN
        List<DictionarySummary> dictionaries = dictionaryService.getDictionaries(USER_ID);

        // THEN each pair appears once with its own counts
        assertThat(dictionaries)
                .extracting(DictionarySummary::targetLanguage)
                .containsExactly(TargetLanguage.DE, TargetLanguage.UK);
        assertThat(dictionaries.get(0).wordCounts().get(VocabularyStatus.KNOWN)).isEqualTo(2L);
        assertThat(dictionaries.get(1).wordCounts().get(VocabularyStatus.REVIEW)).isEqualTo(4L);
        assertThat(dictionaries.get(1).wordCounts().get(VocabularyStatus.UNKNOWN)).isEqualTo(1L);
    }

    @Test
    void getDictionaries_returnsEmptyList_whenUserHasNoWords() {
        // GIVEN the query finds nothing (the repository mock returns an empty list)

        // WHEN
        List<DictionarySummary> dictionaries = dictionaryService.getDictionaries(USER_ID);

        // THEN
        assertThat(dictionaries).isEmpty();
    }

    @Test
    void getDictionaryWords_joinsAllTranslations_whenWordHasSeveralTranslations() {
        // GIVEN a word with two German translations
        WordEntry bank = givenWordEntry(10L, "bank");
        givenDictionaryWords(bank);
        when(translationRepository.findByWordEntryInAndTargetLanguage(anyCollection(), eq(TargetLanguage.DE)))
                .thenReturn(List.of(
                        new Translation(bank, TargetLanguage.DE, "Bank"),
                        new Translation(bank, TargetLanguage.DE, "Ufer")));

        // WHEN
        List<DictionaryWord> words = dictionaryService
                .getDictionaryWords(USER_ID, SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.UNKNOWN);

        // THEN the translations are shown together
        assertThat(words).containsExactly(new DictionaryWord("bank", "Bank, Ufer"));
    }

    @Test
    void getDictionaryWords_returnsEmptyTranslation_whenWordHasNoTranslation() {
        // GIVEN a tracked word without a stored translation
        WordEntry hello = givenWordEntry(11L, "hello");
        givenDictionaryWords(hello);

        // WHEN
        List<DictionaryWord> words = dictionaryService
                .getDictionaryWords(USER_ID, SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.UNKNOWN);

        // THEN the word is still returned
        assertThat(words).containsExactly(new DictionaryWord("hello", ""));
    }

    @Test
    void getDictionaryWords_keepsOrderOfRepository_whenSeveralWordsAreReturned() {
        // GIVEN two words, the repository returns the newest first
        WordEntry world = givenWordEntry(12L, "world");
        WordEntry hello = givenWordEntry(13L, "hello");
        givenDictionaryWords(world, hello);
        when(translationRepository.findByWordEntryInAndTargetLanguage(anyCollection(), eq(TargetLanguage.DE)))
                .thenReturn(List.of(
                        new Translation(hello, TargetLanguage.DE, "hallo"),
                        new Translation(world, TargetLanguage.DE, "Welt")));

        // WHEN
        List<DictionaryWord> words = dictionaryService
                .getDictionaryWords(USER_ID, SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.UNKNOWN);

        // THEN the order is kept and every word gets its own translation
        assertThat(words).containsExactly(
                new DictionaryWord("world", "Welt"),
                new DictionaryWord("hello", "hallo"));
    }

    @Test
    void getDictionaryWords_returnsEmptyList_withoutQueryingTranslations_whenNoWordHasTheStatus() {
        // GIVEN the user has no words with this status (the repository mock finds none)

        // WHEN
        List<DictionaryWord> words = dictionaryService.getDictionaryWords(
                USER_ID, SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.REVIEW);

        // THEN
        assertThat(words).isEmpty();
        verifyNoInteractions(translationRepository);
    }

    @Test
    void deleteDictionary_deletesLanguagePairOfTheUser() {
        // WHEN
        dictionaryService.deleteDictionary(USER_ID, SourceLanguage.EN, TargetLanguage.DE);

        // THEN all words of this user for the language pair are deleted
        verify(userVocabularyRepository).deleteLanguagePair(USER_ID, SourceLanguage.EN, TargetLanguage.DE);
    }

    // Word entries get their ids from the database, so the tests set them by hand
    private WordEntry givenWordEntry(Long id, String lemma) {
        WordEntry wordEntry = new WordEntry(SourceLanguage.EN, lemma);
        ReflectionTestUtils.setField(wordEntry, "id", id);
        return wordEntry;
    }

    private void givenDictionaryWords(WordEntry... wordEntries) {
        AppUser user = new AppUser("user@example.com");
        List<UserVocabulary> userVocabularies = Arrays.stream(wordEntries)
                .map(wordEntry -> new UserVocabulary(user, wordEntry, TargetLanguage.DE, VocabularyStatus.UNKNOWN))
                .toList();
        when(userVocabularyRepository.findUserVocabularies(
                USER_ID, SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.UNKNOWN))
                .thenReturn(userVocabularies);
    }
}
