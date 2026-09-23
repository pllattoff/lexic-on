package com.lexicon.backend.dto;

public record TextToken(
        int start,
        int end,
        String lemma
) {
}