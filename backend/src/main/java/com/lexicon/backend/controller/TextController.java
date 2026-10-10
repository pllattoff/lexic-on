package com.lexicon.backend.controller;

import com.lexicon.backend.dto.ProcessTextRequest;
import com.lexicon.backend.dto.ProcessedText;
import com.lexicon.backend.security.AppOAuth2User;
import com.lexicon.backend.service.TextProcessingService;
import com.lexicon.backend.service.TranslationService;
import com.lexicon.backend.service.VocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/text")
@RequiredArgsConstructor
public class TextController {

    private final TextProcessingService textProcessingService;
    private final TranslationService translationService;
    private final VocabularyService vocabularyService;

    // For unauthenticated requests, Spring Security provides no user; vocabulary statuses are only added for logged-in users
    @PostMapping("/process")
    public ProcessedText process(@AuthenticationPrincipal AppOAuth2User user,
                                 @Valid @RequestBody ProcessTextRequest request) {
        ProcessedText processedText = textProcessingService.process(request.text());
        ProcessedText translatedText = translationService.attachTranslations(processedText, request.sourceLanguage(), request.targetLanguage());

        if (user == null) {
            return translatedText;
        }

        return vocabularyService.attachStatuses(translatedText, request.sourceLanguage(), request.targetLanguage(), user.getAppUserId());
    }
}