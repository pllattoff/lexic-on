package com.lexicon.backend.model;

import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.VocabularyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_vocabulary")
@Getter
@NoArgsConstructor
public class UserVocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser appUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_entry_id", nullable = false)
    private WordEntry wordEntry;

    @Column(name = "target_language", nullable = false, length = 2)
    private TargetLanguage targetLanguage;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VocabularyStatus status;

    public UserVocabulary(AppUser appUser, WordEntry wordEntry, TargetLanguage targetLanguage, VocabularyStatus status) {
        this.appUser = appUser;
        this.wordEntry = wordEntry;
        this.targetLanguage = targetLanguage;
        this.status = status;
    }
}