package com.lexicon.backend.repository;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.model.WordEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WordEntryRepository extends JpaRepository<WordEntry, Long> {
    List<WordEntry> findByLemmaInAndLanguage(Collection<String> lemmas, SourceLanguage language);

    Optional<WordEntry> findByLemmaAndLanguage(String lemma, SourceLanguage language);
}