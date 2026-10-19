package com.lexicon.backend.exception;

public class VerifiedEmailNotFoundException extends RuntimeException {

    public VerifiedEmailNotFoundException(String message) {
        super(message);
    }
}
