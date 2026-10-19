package com.lexicon.backend.service;

import com.lexicon.backend.dto.LanguagePairStatusCount;
import com.lexicon.backend.dto.DictionarySummary;
import com.lexicon.backend.dto.DictionaryWord;
import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.VocabularyStatus;
import com.lexicon.backend.model.Translation;
import com.lexicon.backend.model.UserVocabulary;
import com.lexicon.backend.model.WordEntry;
import com.lexicon.backend.repository.TranslationRepository;
import com.lexicon.backend.repository.UserVocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final UserVocabularyRepository userVocabularyRepository;
    private final TranslationRepository translationRepository;

    // Returns each language pair the user has words in, with counts grouped by status
    @Transactional(readOnly = true)
    public List<DictionarySummary> getDictionaries(Long appUserId) {
        List<LanguagePairStatusCount> languagePairStatusCounts =
                userVocabularyRepository.countWordsByLanguagePairAndStatus(appUserId);

        // Provides complete status counts for each language pair, including zero counts
        Map<LanguagePair, Map<VocabularyStatus, Long>> completeStatusCountsByPair = new LinkedHashMap<>();

        for (LanguagePairStatusCount languagePairStatusCount : languagePairStatusCounts) {
            LanguagePair languagePair = new LanguagePair(
                    languagePairStatusCount.sourceLanguage(),
                    languagePairStatusCount.targetLanguage()
            );

            completeStatusCountsByPair.computeIfAbsent(languagePair, pair -> createStatusCountsWithZeros())
                    .put(languagePairStatusCount.status(), languagePairStatusCount.wordCount());
        }

        return completeStatusCountsByPair.entrySet().stream()
                .map(entry -> new DictionarySummary(
                        entry.getKey().sourceLanguage(),
                        entry.getKey().targetLanguage(),
                        entry.getValue()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DictionaryWord> getDictionaryWords(Long appUserId, SourceLanguage sourceLanguage,
                                                   TargetLanguage targetLanguage, VocabularyStatus status) {
        List<UserVocabulary> userVocabularies =
                userVocabularyRepository.findUserVocabularies(appUserId, sourceLanguage, targetLanguage, status);

        if (userVocabularies.isEmpty()) {
            return List.of();
        }

        List<WordEntry> wordEntries = userVocabularies.stream()
                .map(UserVocabulary::getWordEntry)
                .toList();

        // Groups translations by word entry and combines them into a single string.
        Map<Long, String> translationsByWordEntryId = translationRepository
                .findByWordEntryInAndTargetLanguageOrderById(wordEntries, targetLanguage).stream()
                .collect(Collectors.groupingBy(
                        // Hibernate can retrieve the ID from a LAZY proxy without loading the associated entity
                        translation -> translation.getWordEntry().getId(),
                        Collectors.mapping(Translation::getTranslation, Collectors.joining(", "))));

        return userVocabularies.stream()
                .map(userVocabulary -> new DictionaryWord(
                        userVocabulary.getWordEntry().getLemma(),
                        translationsByWordEntryId.getOrDefault(userVocabulary.getWordEntry().getId(), "")))
                .toList();
    }

    // Removes all words of the user for the language pair, whatever their status
    @Transactional
    public void deleteDictionary(Long appUserId, SourceLanguage sourceLanguage, TargetLanguage targetLanguage) {
        userVocabularyRepository.deleteLanguagePair(appUserId, sourceLanguage, targetLanguage);
    }

    private Map<VocabularyStatus, Long> createStatusCountsWithZeros() {
        Map<VocabularyStatus, Long> wordCounts = new EnumMap<>(VocabularyStatus.class);
        for (VocabularyStatus status : VocabularyStatus.values()) {
            wordCounts.put(status, 0L);
        }
        return wordCounts;
    }

    private record LanguagePair(SourceLanguage sourceLanguage, TargetLanguage targetLanguage) {
    }
}