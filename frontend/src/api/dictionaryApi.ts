import { fetchWithCsrf } from "../lib/csrf.ts";
import type { SourceLanguage, TargetLanguage } from "../lib/languages.ts";
import type { DictionarySummary, DictionaryWord } from "../types/dictionary.ts";
import type { VocabularyStatus } from "../types/text.ts";

// Returns the user's dictionaries as language pairs with word counts for each status
export async function getDictionaries(): Promise<DictionarySummary[]> {
    const res = await fetch("/api/dictionaries");

    if (!res.ok) {
        throw new Error(`Loading dictionaries failed: ${res.status}`);
    }

    return res.json();
}

// Removes all words of the language pair from the user's vocabulary, whatever their status
export async function deleteDictionary(
    sourceLanguage: SourceLanguage,
    targetLanguage: TargetLanguage,
): Promise<void> {
    const res = await fetchWithCsrf(`/api/dictionaries/${sourceLanguage}/${targetLanguage}`, {
        method: "DELETE",
    });

    if (!res.ok) {
        throw new Error(`Deleting dictionary failed: ${res.status}`);
    }
}

// Returns words and their translations for the given language pair and status, ordered by creation date
export async function getDictionaryWords(
    sourceLanguage: SourceLanguage,
    targetLanguage: TargetLanguage,
    status: VocabularyStatus,
): Promise<DictionaryWord[]> {
    const res = await fetch(`/api/dictionaries/${sourceLanguage}/${targetLanguage}/words?status=${status}`);

    if (!res.ok) {
        throw new Error(`Loading dictionary words failed: ${res.status}`);
    }

    return res.json();
}