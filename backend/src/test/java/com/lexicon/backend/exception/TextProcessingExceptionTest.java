package com.lexicon.backend.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextProcessingExceptionTest {

    @Test
    void constructor_setsMessage_whenGivenMessageOnly() {
        // GIVEN
        String message = "error message";

        // WHEN
        TextProcessingException exception = new TextProcessingException(message);

        // THEN
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_setsMessageAndCause_whenGivenBoth() {
        // GIVEN
        String message = "error message";
        Throwable cause = new RuntimeException("root cause");

        // WHEN
        TextProcessingException exception = new TextProcessingException(message, cause);

        // THEN
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
