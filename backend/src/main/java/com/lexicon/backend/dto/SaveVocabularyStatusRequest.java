package com.lexicon.backend.dto;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.VocabularyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveVocabularyStatusRequest(
        @NotBlank String lemma,
        @NotNull SourceLanguage sourceLanguage,
        @NotNull TargetLanguage targetLanguage,
        @NotNull VocabularyStatus status
) {
}