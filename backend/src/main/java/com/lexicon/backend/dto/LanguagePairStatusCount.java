package com.lexicon.backend.dto;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.VocabularyStatus;

// Aggregated word counts retrieved from the database
public record LanguagePairStatusCount(
        SourceLanguage sourceLanguage,
        TargetLanguage targetLanguage,
        VocabularyStatus status,
        long wordCount
) {
}