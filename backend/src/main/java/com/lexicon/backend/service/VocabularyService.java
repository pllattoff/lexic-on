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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final WordEntryRepository wordEntryRepository;
    private final UserVocabularyRepository userVocabularyRepository;
    private final AppUserRepository appUserRepository;

    // A lemma without a stored status (never clicked) keeps a null status
    @Transactional(readOnly = true)
    public ProcessedText attachStatuses(ProcessedText processedText, SourceLanguage sourceLanguage,
                                        TargetLanguage targetLanguage, Long appUserId) {
        Set<String> lemmas = processedText.tokens().stream()
                .map(TextToken::lemma)
                .collect(Collectors.toSet());

        List<WordEntry> wordEntries = wordEntryRepository.findByLemmaInAndLanguage(lemmas, sourceLanguage);

        Map<String, VocabularyStatus> statusesByLemma = userVocabularyRepository
                .findByAppUserIdAndTargetLanguageAndWordEntryIn(appUserId, targetLanguage, wordEntries).stream()
                .collect(Collectors.toMap(
                        userVocabulary -> userVocabulary.getWordEntry().getLemma(),
                        UserVocabulary::getStatus));

        List<TextToken> tokensWithStatuses = processedText.tokens().stream()
                .map(token -> new TextToken(
                        token.start(),
                        token.end(),
                        token.lemma(),
                        token.translation(),
                        statusesByLemma.get(token.lemma())
                ))
                .toList();

        return new ProcessedText(processedText.text(), tokensWithStatuses);
    }

    // Creates a user vocabulary entry if it does not exist, otherwise updates its status
    @Transactional
    public VocabularyStatus saveStatus(Long appUserId, String lemma, SourceLanguage sourceLanguage,
                                       TargetLanguage targetLanguage, VocabularyStatus status) {
        WordEntry wordEntry = findWordEntry(lemma, sourceLanguage);

        UserVocabulary userVocabulary = userVocabularyRepository
                .findByAppUserIdAndWordEntryAndTargetLanguage(appUserId, wordEntry, targetLanguage)
                .orElseGet(() -> {
                    // Get a JPA reference to the user; only the user ID is needed to save UserVocabulary in the database
                    AppUser appUser = appUserRepository.getReferenceById(appUserId);
                    return new UserVocabulary(appUser, wordEntry, targetLanguage, status);
                });

        userVocabulary.setStatus(status);
        userVocabularyRepository.save(userVocabulary);

        return userVocabulary.getStatus();
    }

    @Transactional
    public void deleteStatus(Long appUserId, String lemma, SourceLanguage sourceLanguage, TargetLanguage targetLanguage) {
        WordEntry wordEntry = findWordEntry(lemma, sourceLanguage);

        userVocabularyRepository.deleteByAppUserIdAndWordEntryAndTargetLanguage(appUserId, wordEntry, targetLanguage);
    }

    private WordEntry findWordEntry(String lemma, SourceLanguage sourceLanguage) {
        return wordEntryRepository.findByLemmaAndLanguage(lemma, sourceLanguage)
                .orElseThrow(() -> new WordEntryNotFoundException("No word entry for lemma: " + lemma));
    }
}