package com.lexicon.backend.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerifiedEmailNotFoundExceptionTest {

    @Test
    void constructor_setsMessage_whenGivenMessage() {
        // GIVEN
        String message = "error message";

        // WHEN
        VerifiedEmailNotFoundException exception = new VerifiedEmailNotFoundException(message);

        // THEN
        assertThat(exception.getMessage()).isEqualTo(message);
    }
}
