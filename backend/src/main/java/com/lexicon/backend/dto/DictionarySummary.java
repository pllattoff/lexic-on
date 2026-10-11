package com.lexicon.backend.dto;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.VocabularyStatus;

import java.util.Map;

public record DictionarySummary(
        SourceLanguage sourceLanguage,
        TargetLanguage targetLanguage,
        Map<VocabularyStatus, Long> wordCounts
) {
}