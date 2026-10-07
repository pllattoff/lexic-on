package com.lexicon.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WordEntryNotFoundException extends RuntimeException {

    public WordEntryNotFoundException(String message) {
        super(message);
    }
}