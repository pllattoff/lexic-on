package com.lexicon.backend.dto;

import com.lexicon.backend.enums.VocabularyStatus;

public record VocabularyStatusResponse(
        VocabularyStatus status
) {
}