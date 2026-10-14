import { useEffect, useState } from "react";
import { useLocation } from "react-router";
import { getDictionaries } from "../api/dictionaryApi.ts";
import type { SourceLanguage, TargetLanguage } from "../lib/languages.ts";
import type { DictionarySummary } from "../types/dictionary.ts";

export type VocabularyChange = "saved" | "deleted";

// Manages the user's dictionaries and reloads them when needed
export function useDictionaries(isAuthenticated: boolean) {
    const [dictionaries, setDictionaries] = useState<DictionarySummary[] | null>(null);
    // Incrementing this counter triggers the effect to reload the dictionaries
    const [reloadCounter, setReloadCounter] = useState(0);
    const { pathname } = useLocation();

    // Track navigation in the Dictionaries section to refresh dictionary counts when the path changes
    const dictionariesPathname = pathname.startsWith("/dictionaries") ? pathname : null;

    useEffect(() => {
        if (!isAuthenticated) return;

        // If a new request starts before the previous one finishes, ignore the previous response
        // to prevent outdated data from overwriting newer data.
        let ignoreResponse = false;

        async function loadDictionaries() {
            try {
                const loadedDictionaries = await getDictionaries();

                if (!ignoreResponse) {
                    setDictionaries(loadedDictionaries);
                }
            } catch (error) {
                console.error("Failed to load dictionaries:", error);
            }
        }

        void loadDictionaries();

        // Cleanup function returned to React to mark the previous request's response as outdated
        return () => {
            ignoreResponse = true;
        };
    }, [isAuthenticated, dictionariesPathname, reloadCounter]);

    // Reload the dictionaries when a new language pair is created or a word is deleted.
    // A pair is created when its first word is saved.
    function handleVocabularyChange(
        sourceLanguage: SourceLanguage,
        targetLanguage: TargetLanguage,
        change: VocabularyChange,
    ) {
        const pairExists = dictionaries?.some(
            (dictionary) => dictionary.sourceLanguage === sourceLanguage && dictionary.targetLanguage === targetLanguage,
        );

        if (change === "deleted" || !pairExists) {
            setReloadCounter((counter) => counter + 1);
        }
    }

    return { dictionaries, handleVocabularyChange };
}