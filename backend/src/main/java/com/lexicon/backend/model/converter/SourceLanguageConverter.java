package com.lexicon.backend.model.converter;

import com.lexicon.backend.enums.SourceLanguage;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SourceLanguageConverter implements AttributeConverter<SourceLanguage, String> {

    @Override
    public String convertToDatabaseColumn(SourceLanguage attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public SourceLanguage convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SourceLanguage.fromCode(dbData);
    }
}