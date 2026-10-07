package com.lexicon.backend.dto;

import com.lexicon.backend.enums.VocabularyStatus;

public record TextToken(
        int start,
        int end,
        String lemma,
        String translation,
        VocabularyStatus status
) {
}