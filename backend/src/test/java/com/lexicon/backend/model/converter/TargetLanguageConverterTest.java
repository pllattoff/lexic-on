package com.lexicon.backend.model.converter;

import com.lexicon.backend.enums.TargetLanguage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TargetLanguageConverterTest {

    private final TargetLanguageConverter targetLanguageConverter = new TargetLanguageConverter();

    @Test
    void convertToDatabaseColumn_returnsNull_whenAttributeIsNull() {
        // GIVEN
        TargetLanguage attribute = null;

        // WHEN
        String dbData = targetLanguageConverter.convertToDatabaseColumn(attribute);

        // THEN
        assertThat(dbData).isNull();
    }

    @Test
    void convertToEntityAttribute_returnsNull_whenDbDataIsNull() {
        // GIVEN
        String dbData = null;

        // WHEN
        TargetLanguage attribute = targetLanguageConverter.convertToEntityAttribute(dbData);

        // THEN
        assertThat(attribute).isNull();
    }
}
