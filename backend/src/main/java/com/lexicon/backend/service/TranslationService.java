package com.lexicon.backend.service;

import com.lexicon.backend.dto.ProcessedText;
import com.lexicon.backend.dto.TextToken;
import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.model.Translation;
import com.lexicon.backend.model.WordEntry;
import com.lexicon.backend.repository.TranslationRepository;
import com.lexicon.backend.repository.WordEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final WordEntryRepository wordEntryRepository;
    private final TranslationRepository translationRepository;
    private final AzureTranslatorClient azureTranslatorClient;
    private final TranslationPersistenceService translationPersistenceService;

    public ProcessedText attachTranslations(ProcessedText processedText, SourceLanguage sourceLanguage, TargetLanguage targetLanguage) {
        // LinkedHashSet keeps the order of first appearance, so the Azure batch order is deterministic
        Set<String> lemmas = processedText.tokens().stream()
                .map(TextToken::lemma)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, String> translationsByLemma = translateLemmas(lemmas, sourceLanguage, targetLanguage);

        List<TextToken> translatedTokens = processedText.tokens().stream()
                .map(token -> new TextToken(
                        token.start(),
                        token.end(),
                        token.lemma(),
                        translationsByLemma.get(token.lemma())
                ))
                .toList();

        return new ProcessedText(processedText.text(), translatedTokens);
    }

    private Map<String, String> translateLemmas(Set<String> lemmas, SourceLanguage sourceLanguage, TargetLanguage targetLanguage) {

        Map<String, String> translationsByLemma = findExistingTranslations(lemmas, sourceLanguage, targetLanguage);

        List<String> lemmasWithoutTranslations = lemmas.stream()
                .filter(lemma -> !translationsByLemma.containsKey(lemma))
                .toList();

        if (!lemmasWithoutTranslations.isEmpty()) {
            Map<String, String> newTranslationsByLemma = fetchAndSaveMissingTranslations(lemmasWithoutTranslations, sourceLanguage, targetLanguage);
            translationsByLemma.putAll(newTranslationsByLemma);
        }

        return translationsByLemma;
    }

    private Map<String, String> findExistingTranslations(Set<String> lemmas, SourceLanguage sourceLanguage, TargetLanguage targetLanguage) {
        List<WordEntry> wordEntries =
                wordEntryRepository.findByLemmaInAndLanguage(lemmas, sourceLanguage);

        List<Translation> translations =
                translationRepository.findByWordEntryInAndTargetLanguage(wordEntries, targetLanguage);

        Map<String, String> translationsByLemma = new HashMap<>();

        for (Translation translation : translations) {
            translationsByLemma.put(
                    translation.getWordEntry().getLemma(),
                    translation.getTranslation()
            );
        }

        return translationsByLemma;
    }

    private Map<String, String> fetchAndSaveMissingTranslations(List<String> lemmasWithoutTranslations, SourceLanguage sourceLanguage, TargetLanguage targetLanguage) {
        List<String> translations = azureTranslatorClient.translate(lemmasWithoutTranslations, sourceLanguage, targetLanguage);

        Map<String, String> translationsByLemma = new HashMap<>();

        for (int i = 0; i < lemmasWithoutTranslations.size(); i++) {
            translationsByLemma.put(
                    lemmasWithoutTranslations.get(i),
                    translations.get(i)
            );
        }

        translationPersistenceService.saveTranslations(translationsByLemma, sourceLanguage, targetLanguage);

        return translationsByLemma;
    }
}