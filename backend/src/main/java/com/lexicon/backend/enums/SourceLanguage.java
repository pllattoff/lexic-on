package com.lexicon.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum SourceLanguage {
    EN("en");

    // ISO language code used by Jackson for JSON and by JPA AttributeConverters for database persistence
    private final String code;

    SourceLanguage(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static SourceLanguage fromCode(String code) {
        return Arrays.stream(values())
                .filter(language -> language.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown source language code: " + code));
    }
}