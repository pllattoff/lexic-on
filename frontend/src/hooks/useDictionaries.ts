import { useEffect, useState } from "react";
import { useLocation } from "react-router";
import { getDictionaries } from "../api/dictionaryApi.ts";
import type { SourceLanguage, TargetLanguage } from "../lib/languages.ts";
import type { DictionarySummary } from "../types/dictionary.ts";

export type VocabularyChange = "saved" | "deleted";

// The user's dictionaries for the sidebar and the Dictionaries pages.
// null until the first response arrives
export function useDictionaries(isAuthenticated: boolean) {
    const [dictionaries, setDictionaries] = useState<DictionarySummary[] | null>(null);
    // Changing it makes the effect below load the dictionaries again
    const [reloadCounter, setReloadCounter] = useState(0);
    const { pathname } = useLocation();

    // The address changes on every move inside the Dictionaries section, so the counts are fresh on each visit
    const dictionariesPathname = pathname.startsWith("/dictionaries") ? pathname : null;

    useEffect(() => {
        if (!isAuthenticated) return;

        // Ignore the response if a newer request was started meanwhile
        let ignore = false;

        async function loadDictionaries() {
            try {
                const loadedDictionaries = await getDictionaries();
                if (!ignore) setDictionaries(loadedDictionaries);
            } catch (error) {
                console.error("Failed to load dictionaries:", error);
            }
        }

        void loadDictionaries();

        return () => {
            ignore = true;
        };
    }, [isAuthenticated, dictionariesPathname, reloadCounter]);

    // A new language pair appears only with its first saved word, and a pair can disappear only
    // when a word is deleted: reload in these two cases and skip the request otherwise
    function handleVocabularyChange(
        sourceLanguage: SourceLanguage,
        targetLanguage: TargetLanguage,
        change: VocabularyChange,
    ) {
        const isKnownPair = dictionaries?.some(
            (dictionary) => dictionary.sourceLanguage === sourceLanguage && dictionary.targetLanguage === targetLanguage,
        );

        if (change === "deleted" || !isKnownPair) {
            setReloadCounter((counter) => counter + 1);
        }
    }

    return { dictionaries, handleVocabularyChange };
}