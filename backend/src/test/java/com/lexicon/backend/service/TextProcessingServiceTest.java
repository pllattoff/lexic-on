package com.lexicon.backend.service;

import com.lexicon.backend.dto.ProcessedText;
import com.lexicon.backend.dto.TextToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextProcessingServiceTest {

    private final TextProcessingService textProcessingService = new TextProcessingService();

    @Test
    void process_returnsWordTokensWithCorrectOffsets_whenProcessingText() {
        // GIVEN
        String text = "He went for a run with his friends.";

        // WHEN
        ProcessedText result = textProcessingService.process(text);

        // THEN each token's offsets point to the correct word in the original text
        List<TextToken> tokens = result.tokens();
        assertThat(tokens)
                .extracting(token -> text.substring(token.start(), token.end()))
                .containsExactly("He", "went", "for", "a", "run", "with", "his", "friends");
    }

    @Test
    void process_returnsCorrectLemmas_whenWordsAreNotInBaseForm() {
        // GIVEN
        String text = "He went for a run with his friends.";

        // WHEN
        ProcessedText result = textProcessingService.process(text);

        // THEN non-base forms are mapped to their base forms
        List<TextToken> tokens = result.tokens();

        assertThat(tokens)
                .filteredOn(token -> text.substring(token.start(), token.end()).equals("went"))
                .extracting(TextToken::lemma)
                .containsExactly("go");

        assertThat(tokens)
                .filteredOn(token -> text.substring(token.start(), token.end()).equals("friends"))
                .extracting(TextToken::lemma)
                .containsExactly("friend");
    }

    @Test
    void process_excludesTokensWithoutLetters_whenProcessingText() {
        // GIVEN a sentence containing punctuation and digits
        String text = "Hello, world! 123";

        // WHEN
        ProcessedText result = textProcessingService.process(text);

        // THEN tokens without letters are excluded
        List<String> words = result.tokens().stream()
                .map(token -> text.substring(token.start(), token.end()))
                .toList();

        assertThat(words).containsExactly("Hello", "world");
    }

    @Test
    void process_returnsNoTokens_whenTextIsBlank() {
        // GIVEN
        String text = "   ";

        // WHEN
        ProcessedText result = textProcessingService.process(text);

        // THEN
        assertThat(result.tokens()).isEmpty();
    }
}
