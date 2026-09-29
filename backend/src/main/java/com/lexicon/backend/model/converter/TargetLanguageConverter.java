package com.lexicon.backend.model.converter;

import com.lexicon.backend.enums.TargetLanguage;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TargetLanguageConverter implements AttributeConverter<TargetLanguage, String> {

    @Override
    public String convertToDatabaseColumn(TargetLanguage attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public TargetLanguage convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TargetLanguage.fromCode(dbData);
    }
}