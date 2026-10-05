package com.lexicon.backend.model;

import com.lexicon.backend.enums.SourceLanguage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "word_entry")
@Getter
@NoArgsConstructor
public class WordEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2)
    private SourceLanguage language;

    @Column(nullable = false, length = 100)
    private String lemma;

    public WordEntry(SourceLanguage language, String lemma) {
        this.language = language;
        this.lemma = lemma;
    }
}