package com.lexicon.backend.service;

import com.lexicon.backend.dto.ProcessedText;
import com.lexicon.backend.dto.TextToken;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TextProcessingService {
    // Matches a word starting with a letter, followed by letters or combining marks
    private static final Pattern WORD = Pattern.compile("\\p{L}[\\p{L}\\p{Mn}]*");

    public ProcessedText process(String text) {
        List<TextToken> tokens = new ArrayList<>();

        Matcher matcher = WORD.matcher(text);
        while (matcher.find()) {
            tokens.add(new TextToken(
                    matcher.start(),
                    matcher.end(),
                    matcher.group().toLowerCase()
            ));
        }

        return new ProcessedText(text, tokens);
    }
}