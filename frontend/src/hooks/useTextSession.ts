import { useEffect, useState } from "react";
import type { SourceLanguage, TargetLanguage } from "../lib/languages.ts";
import { INPUT_TEXT_STORAGE_KEY } from "../lib/storage.ts";
import { DEFAULT_HIDDEN_HIGHLIGHTS, type HighlightKey } from "../lib/vocabulary.ts";
import type { ProcessedText } from "../types/text.ts";
import type { VocabularyStatus } from "../types/vocabulary.ts";

// The current text of the user.
// Manages the text-related states shared across pages, preserving them when navigating between sections.
export function useTextSession() {
    const [inputText, setInputText] = useState(
        () => sessionStorage.getItem(INPUT_TEXT_STORAGE_KEY) ?? "",
    );
    const [sourceLanguage, setSourceLanguage] = useState<SourceLanguage>("en");
    const [targetLanguage, setTargetLanguage] = useState<TargetLanguage>("de");
    const [processedText, setProcessedText] = useState<ProcessedText | null>(null);
    // Kinds of words that are not highlighted in the text, chosen with the stats bar tiles
    const [hiddenHighlights, setHiddenHighlights] = useState<ReadonlySet<HighlightKey>>(
        () => new Set(DEFAULT_HIDDEN_HIGHLIGHTS),
    );

    // Save the input text to sessionStorage for recovery after page reloads or GitHub login redirect
    useEffect(() => {
        sessionStorage.setItem(INPUT_TEXT_STORAGE_KEY, inputText);
    }, [inputText]);

    function toggleHighlight(key: HighlightKey) {
        setHiddenHighlights((current) => {
            const next = new Set(current);
            if (!next.delete(key)) next.add(key);
            return next;
        });
    }

    // Update all occurrences of the lemmas in processedText state without processing the text again
    function updateLemmasStatus(lemmas: readonly string[], status: VocabularyStatus | null) {
        const changedLemmas = new Set(lemmas);

        setProcessedText((current) => {
            if (!current) return current;

            const tokens = current.tokens.map((token) => {
                if (!changedLemmas.has(token.lemma)) return token;

                // Copy the token and override its status property with the new value
                return { ...token, status };
            });

            return { ...current, tokens };
        });
    }

    // Words got a new status: the open text shows it if the text is of the same language pair
    function updateLemmasStatusOfPair(
        changedSource: SourceLanguage,
        changedTarget: TargetLanguage,
        lemmas: readonly string[],
        status: VocabularyStatus | null,
    ) {
        if (sourceLanguage !== changedSource || targetLanguage !== changedTarget) return;

        updateLemmasStatus(lemmas, status);
    }

    // The words of a deleted dictionary are not tracked anymore, so an open text of that language pair
    // must not keep showing their statuses
    function resetStatuses(deletedSource: SourceLanguage, deletedTarget: TargetLanguage) {
        if (sourceLanguage !== deletedSource || targetLanguage !== deletedTarget) return;

        setProcessedText((current) => {
            if (!current) return current;

            return { ...current, tokens: current.tokens.map((token) => ({ ...token, status: null })) };
        });
    }

    function clearText() {
        setInputText("");
        setProcessedText(null);
        setHiddenHighlights(new Set(DEFAULT_HIDDEN_HIGHLIGHTS));
    }

    return {
        inputText,
        setInputText,
        sourceLanguage,
        setSourceLanguage,
        targetLanguage,
        setTargetLanguage,
        processedText,
        setProcessedText,
        hiddenHighlights,
        toggleHighlight,
        updateLemmasStatusOfPair,
        resetStatuses,
        clearText,
    };
}

// typeof gets the function's type, while ReturnType extracts the type of the value it returns
export type TextSession = ReturnType<typeof useTextSession>;