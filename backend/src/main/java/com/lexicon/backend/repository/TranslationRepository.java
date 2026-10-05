package com.lexicon.backend.repository;

import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.model.Translation;
import com.lexicon.backend.model.WordEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TranslationRepository extends JpaRepository<Translation, Long> {
    List<Translation> findByWordEntryInAndTargetLanguage(Collection<WordEntry> wordEntries, TargetLanguage targetLanguage);
}