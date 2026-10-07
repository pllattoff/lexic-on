package com.lexicon.backend.dto;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeleteVocabularyStatusRequest(
        @NotBlank String lemma,
        @NotNull SourceLanguage sourceLanguage,
        @NotNull TargetLanguage targetLanguage
) {
}