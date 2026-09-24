package com.lexicon.backend.controller;

import com.lexicon.backend.dto.ProcessTextRequest;
import com.lexicon.backend.dto.ProcessedText;
import com.lexicon.backend.service.TextProcessingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/text")
@RequiredArgsConstructor
public class TextController {

    private final TextProcessingService textProcessingService;

    @PostMapping("/process")
    public ProcessedText process(@Valid @RequestBody ProcessTextRequest request) {
        return textProcessingService.process(request.text());
    }
}