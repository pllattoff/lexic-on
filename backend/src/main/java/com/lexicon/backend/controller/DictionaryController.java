package com.lexicon.backend.controller;

import com.lexicon.backend.dto.DictionarySummary;
import com.lexicon.backend.dto.DictionaryWord;
import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.VocabularyStatus;
import com.lexicon.backend.security.AppOAuth2User;
import com.lexicon.backend.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dictionaries")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping
    public List<DictionarySummary> getDictionaries(@AuthenticationPrincipal AppOAuth2User user) {
        return dictionaryService.getDictionaries(user.getAppUserId());
    }

    // Language codes in path variables are converted to enums by converters registered in WebConfig
    @GetMapping("/{sourceLanguage}/{targetLanguage}/words")
    public List<DictionaryWord> getWords(@AuthenticationPrincipal AppOAuth2User user,
                                         @PathVariable SourceLanguage sourceLanguage,
                                         @PathVariable TargetLanguage targetLanguage,
                                         @RequestParam VocabularyStatus status) {
        return dictionaryService.getDictionaryWords(user.getAppUserId(), sourceLanguage, targetLanguage, status);
    }

    @DeleteMapping("/{sourceLanguage}/{targetLanguage}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDictionary(@AuthenticationPrincipal AppOAuth2User user,
                                 @PathVariable SourceLanguage sourceLanguage,
                                 @PathVariable TargetLanguage targetLanguage) {
        dictionaryService.deleteDictionary(user.getAppUserId(), sourceLanguage, targetLanguage);
    }
}