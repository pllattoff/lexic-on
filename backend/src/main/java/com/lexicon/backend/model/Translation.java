package com.lexicon.backend.model;

import com.lexicon.backend.enums.TargetLanguage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "translation")
@Getter
@NoArgsConstructor
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_entry_id", nullable = false)
    private WordEntry wordEntry;

    @Column(name = "target_language", nullable = false, length = 2)
    private TargetLanguage targetLanguage;

    @Column(nullable = false, length = 100)
    private String translation;

    public Translation(WordEntry wordEntry, TargetLanguage targetLanguage, String translation) {
        this.wordEntry = wordEntry;
        this.targetLanguage = targetLanguage;
        this.translation = translation;
    }
}