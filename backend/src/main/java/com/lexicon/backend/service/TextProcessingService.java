package com.lexicon.backend.service;

import com.lexicon.backend.dto.ProcessedText;
import com.lexicon.backend.dto.TextToken;
import com.lexicon.backend.exception.TextProcessingException;
import org.languagetool.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TextProcessingService {

    // Skip grammar and false-friend rules because this service only uses LanguageTool
    // for tokenization and lemmatization, reducing startup time and memory usage.
    //
    // JLanguageTool is not thread-safe, so each Tomcat worker thread gets its own
    // cached instance instead of creating a new instance for every request.
    private static final ThreadLocal<JLanguageTool> LANGUAGE_TOOL = ThreadLocal.withInitial(() ->
            new JLanguageTool(
                    Languages.getLanguageForShortCode("en-US"),
                    Collections.emptyList(),
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    Collections.emptyList()
            )
    );

    public ProcessedText process(String text) {
        JLanguageTool languageTool = LANGUAGE_TOOL.get();

        List<TextToken> tokens = new ArrayList<>();
        int cursor = 0;

        try {
            for (AnalyzedSentence sentence : languageTool.analyzeText(text)) {
                for (AnalyzedTokenReadings tokenReadings : sentence.getTokensWithoutWhitespace()) {
                    String token = tokenReadings.getToken();

                    // Keep only word tokens
                    if (token.isBlank() || token.chars().noneMatch(Character::isLetter)) {
                        continue;
                    }

                    int start = text.indexOf(token, cursor);
                    if (start < 0) {
                        throw new TextProcessingException("Could not find token in original text: " + token);
                    }

                    int end = start + token.length();

                    tokens.add(new TextToken(
                            start,
                            end,
                            resolveLemma(tokenReadings, token)
                    ));

                    cursor = end;
                }
            }
        } catch (IOException e) {
            throw new TextProcessingException("LanguageTool analysis failed", e);
        }

        return new ProcessedText(text, tokens);
    }

    // Resolve the lemma from the token's possible readings.
    // Prefer a lemma that differs from the original token when available.
    private String resolveLemma(AnalyzedTokenReadings tokenReadings, String token) {
        return tokenReadings.getReadings().stream()
                .map(AnalyzedToken::getLemma)
                .filter(lemma -> lemma != null && !lemma.equalsIgnoreCase(token))
                .findFirst()
                .orElseGet(() -> {
                    String firstLemma = tokenReadings.getReadings().get(0).getLemma();
                    return firstLemma != null ? firstLemma : token;
                });
    }
}