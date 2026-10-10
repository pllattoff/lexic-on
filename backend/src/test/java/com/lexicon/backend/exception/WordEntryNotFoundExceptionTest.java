package com.lexicon.backend.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WordEntryNotFoundExceptionTest {

    @Test
    void constructor_setsMessage_whenGivenMessage() {
        // GIVEN
        String message = "error message";

        // WHEN
        WordEntryNotFoundException exception = new WordEntryNotFoundException(message);

        // THEN
        assertThat(exception.getMessage()).isEqualTo(message);
    }
}