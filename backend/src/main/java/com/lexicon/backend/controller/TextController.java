package com.lexicon.backend.controller;

import com.lexicon.backend.dto.ProcessTextRequest;
import com.lexicon.backend.dto.ProcessedText;
import com.lexicon.backend.service.TextProcessingService;
import com.lexicon.backend.service.TranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/text")
@RequiredArgsConstructor
public class TextController {

    private final TextProcessingService textProcessingService;
    private final TranslationService translationService;

    @PostMapping("/process")
    public ProcessedText process(@Valid @RequestBody ProcessTextRequest request) {
        ProcessedText processedText = textProcessingService.process(request.text());
        return translationService.attachTranslations(processedText, request.sourceLanguage(), request.targetLanguage());
    }
}