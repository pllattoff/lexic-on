package com.lexicon.backend.dto;

import java.util.List;

public record ProcessedText(
        String text,
        List<TextToken> tokens
) {
}