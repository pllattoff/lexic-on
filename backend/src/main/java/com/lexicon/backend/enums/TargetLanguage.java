package com.lexicon.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TargetLanguage {
    DE("de"),
    UK("uk");

    // ISO language code used by Jackson for JSON and by JPA AttributeConverters for database persistence
    private final String code;

    TargetLanguage(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static TargetLanguage fromCode(String code) {
        return Arrays.stream(values())
                .filter(language -> language.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown target language code: " + code));
    }
}