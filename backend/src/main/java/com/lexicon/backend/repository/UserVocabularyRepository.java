package com.lexicon.backend.repository;

import com.lexicon.backend.dto.LanguagePairStatusCount;
import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.VocabularyStatus;
import com.lexicon.backend.model.UserVocabulary;
import com.lexicon.backend.model.WordEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select new com.lexicon.backend.dto.LanguagePairStatusCount(
                    wordEntry.language, userVocabulary.targetLanguage, userVocabulary.status, count(userVocabulary))
            from UserVocabulary userVocabulary
            join userVocabulary.wordEntry wordEntry
            where userVocabulary.appUser.id = :appUserId
            group by wordEntry.language, userVocabulary.targetLanguage, userVocabulary.status
            order by wordEntry.language, userVocabulary.targetLanguage
            """)
    List<LanguagePairStatusCount> countWordsByLanguagePairAndStatus(@Param("appUserId") Long appUserId);

    // Fetches wordEntry eagerly to avoid additional queries when accessing this LAZY relationship
    @Query("""
            select userVocabulary
            from UserVocabulary userVocabulary
            join fetch userVocabulary.wordEntry wordEntry
            where userVocabulary.appUser.id = :appUserId
              and wordEntry.language = :sourceLanguage
              and userVocabulary.targetLanguage = :targetLanguage
              and userVocabulary.status = :status
            order by userVocabulary.createdAt desc, userVocabulary.id desc
            """)
    List<UserVocabulary> findUserVocabularies(@Param("appUserId") Long appUserId,
                                              @Param("sourceLanguage") SourceLanguage sourceLanguage,
                                              @Param("targetLanguage") TargetLanguage targetLanguage,
                                              @Param("status") VocabularyStatus status);
}