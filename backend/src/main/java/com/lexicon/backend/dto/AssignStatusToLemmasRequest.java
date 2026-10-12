package com.lexicon.backend.dto;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.VocabularyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AssignStatusToLemmasRequest(
        @NotEmpty @Size(max = MAX_LEMMAS) List<@NotBlank String> lemmas,
        @NotNull SourceLanguage sourceLanguage,
        @NotNull TargetLanguage targetLanguage,
        @NotNull VocabularyStatus status
) {

    // Limits the size of one request: a text has far fewer different words than this
    public static final int MAX_LEMMAS = 5000;
}
