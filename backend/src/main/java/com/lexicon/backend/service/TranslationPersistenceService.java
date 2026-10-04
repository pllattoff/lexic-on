package com.lexicon.backend.service;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.model.Translation;
import com.lexicon.backend.model.WordEntry;
import com.lexicon.backend.repository.TranslationRepository;
import com.lexicon.backend.repository.WordEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TranslationPersistenceService {

    private final WordEntryRepository wordEntryRepository;
    private final TranslationRepository translationRepository;

    @Transactional
    public void saveTranslations(Map<String, String> translationsByLemma,
                                 SourceLanguage sourceLanguage, TargetLanguage targetLanguage) {

        // Load existing word entries for deduplication
        List<WordEntry> existingWordEntries = wordEntryRepository
                .findByLemmaInAndLanguage(translationsByLemma.keySet(), sourceLanguage);

        Set<String> existingLemmas = existingWordEntries.stream()
                .map(WordEntry::getLemma)
                .collect(Collectors.toSet());

        // Create new word entries
        List<WordEntry> newWordEntries = translationsByLemma.keySet().stream()
                .filter(lemma -> !existingLemmas.contains(lemma))
                .map(lemma -> new WordEntry(sourceLanguage, lemma))
                .toList();

        wordEntryRepository.saveAll(newWordEntries);

        // Combine existing and new word entries for lookup by lemma
        Map<String, WordEntry> wordEntriesByLemma = Stream
                .concat(existingWordEntries.stream(), newWordEntries.stream())
                .collect(Collectors.toMap(WordEntry::getLemma, w -> w));

        // Load existing translations for deduplication
        List<Translation> existingTranslations = translationRepository
                .findByWordEntryInAndTargetLanguage(wordEntriesByLemma.values(), targetLanguage);

        Set<Long> translatedWordEntryIds = existingTranslations.stream()
                .map(translation -> translation.getWordEntry().getId())
                .collect(Collectors.toSet());

        // Create new translations
        List<Translation> newTranslations = new ArrayList<>();

        for (Map.Entry<String, String> lemmaTranslation : translationsByLemma.entrySet()) {
            WordEntry wordEntry = wordEntriesByLemma.get(lemmaTranslation.getKey());

            if (!translatedWordEntryIds.contains(wordEntry.getId())) {
                newTranslations.add(
                        new Translation(wordEntry, targetLanguage, lemmaTranslation.getValue())
                );
            }
        }

        translationRepository.saveAll(newTranslations);
    }
}
