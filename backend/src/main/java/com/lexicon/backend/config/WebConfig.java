package com.lexicon.backend.config;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Configures Spring MVC to handle language codes in endpoint path variables.
    // Registers converters to transform String values into enums using enum methods.
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, SourceLanguage.class, SourceLanguage::fromCode);
        registry.addConverter(String.class, TargetLanguage.class, TargetLanguage::fromCode);
    }
}