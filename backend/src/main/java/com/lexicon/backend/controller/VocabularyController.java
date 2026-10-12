package com.lexicon.backend.controller;

import com.lexicon.backend.dto.AssignStatusToLemmasRequest;
import com.lexicon.backend.dto.AssignedLemmasResponse;
import com.lexicon.backend.dto.DeleteVocabularyStatusRequest;
import com.lexicon.backend.dto.SaveVocabularyStatusRequest;
import com.lexicon.backend.dto.VocabularyStatusResponse;
import com.lexicon.backend.enums.VocabularyStatus;
import com.lexicon.backend.security.AppOAuth2User;
import com.lexicon.backend.service.VocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vocabulary")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;

    @PutMapping
    public VocabularyStatusResponse saveStatus(// Get the authenticated user from Spring Security
                                                @AuthenticationPrincipal AppOAuth2User user,
                                                @Valid @RequestBody SaveVocabularyStatusRequest request) {
        VocabularyStatus status = vocabularyService.saveStatus(
                user.getAppUserId(),
                request.lemma(),
                request.sourceLanguage(),
                request.targetLanguage(),
                request.status());

        return new VocabularyStatusResponse(status);
    }

    // Assigns a vocabulary status to multiple lemmas at once.
    // Existing entries keep their current status; returns the lemmas that were added.
    @PostMapping("/lemmas/status")
    public AssignedLemmasResponse assignStatusToLemmas(@AuthenticationPrincipal AppOAuth2User user,
                                                       @Valid @RequestBody AssignStatusToLemmasRequest request) {
        List<String> assignedLemmas = vocabularyService.assignStatusToLemmas(
                user.getAppUserId(),
                request.lemmas(),
                request.sourceLanguage(),
                request.targetLanguage(),
                request.status());

        return new AssignedLemmasResponse(assignedLemmas);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStatus(@AuthenticationPrincipal AppOAuth2User user,
                             @Valid @RequestBody DeleteVocabularyStatusRequest request) {
        vocabularyService.deleteStatus(
                user.getAppUserId(),
                request.lemma(),
                request.sourceLanguage(),
                request.targetLanguage());
    }
}