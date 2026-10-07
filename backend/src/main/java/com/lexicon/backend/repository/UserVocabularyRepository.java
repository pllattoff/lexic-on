package com.lexicon.backend.repository;

import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.model.UserVocabulary;
import com.lexicon.backend.model.WordEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, Long> {

    List<UserVocabulary> findByAppUserIdAndTargetLanguageAndWordEntryIn(
            Long appUserId, TargetLanguage targetLanguage, Collection<WordEntry> wordEntries);

    Optional<UserVocabulary> findByAppUserIdAndWordEntryAndTargetLanguage(
            Long appUserId, WordEntry wordEntry, TargetLanguage targetLanguage);

    void deleteByAppUserIdAndWordEntryAndTargetLanguage(
            Long appUserId, WordEntry wordEntry, TargetLanguage targetLanguage);
}