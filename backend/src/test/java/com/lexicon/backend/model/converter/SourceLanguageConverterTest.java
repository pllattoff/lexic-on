package com.lexicon.backend.model.converter;

import com.lexicon.backend.enums.SourceLanguage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceLanguageConverterTest {

    private final SourceLanguageConverter sourceLanguageConverter = new SourceLanguageConverter();

    @Test
    void convertToDatabaseColumn_returnsNull_whenAttributeIsNull() {
        // GIVEN
        SourceLanguage attribute = null;

        // WHEN
        String dbData = sourceLanguageConverter.convertToDatabaseColumn(attribute);

        // THEN
        assertThat(dbData).isNull();
    }

    @Test
    void convertToEntityAttribute_returnsNull_whenDbDataIsNull() {
        // GIVEN
        String dbData = null;

        // WHEN
        SourceLanguage attribute = sourceLanguageConverter.convertToEntityAttribute(dbData);

        // THEN
        assertThat(attribute).isNull();
    }
}
