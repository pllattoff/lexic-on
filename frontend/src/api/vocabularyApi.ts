import { fetchWithCsrf } from "../lib/csrf.ts";
import type { SourceLanguage, TargetLanguage } from "../lib/languages.ts";
import type { VocabularyStatus } from "../types/text.ts";

// Creates the word's vocabulary entry or changes its status, returns the saved status
export async function saveVocabularyStatus(
    lemma: string,
    sourceLanguage: SourceLanguage,
    targetLanguage: TargetLanguage,
    status: VocabularyStatus,
): Promise<VocabularyStatus> {
    const res = await fetchWithCsrf("/api/vocabulary", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ lemma, sourceLanguage, targetLanguage, status }),
    });

    if (!res.ok) {
        throw new Error(`Saving vocabulary status failed: ${res.status}`);
    }

    const body: { status: VocabularyStatus } = await res.json();

    return body.status;
}

// Adds the words the user does not track yet, already tracked ones keep their status.
// Returns the lemmas that were actually added.
export async function assignStatusToLemmas(
    lemmas: string[],
    sourceLanguage: SourceLanguage,
    targetLanguage: TargetLanguage,
    status: VocabularyStatus,
): Promise<string[]> {
    const res = await fetchWithCsrf("/api/vocabulary/lemmas/status", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ lemmas, sourceLanguage, targetLanguage, status }),
    });

    if (!res.ok) {
        throw new Error(`Adding vocabulary words failed: ${res.status}`);
    }

    const body: { lemmas: string[] } = await res.json();

    return body.lemmas;
}

// Removes the word from the user's vocabulary
export async function deleteVocabularyStatus(
    lemma: string,
    sourceLanguage: SourceLanguage,
    targetLanguage: TargetLanguage,
): Promise<void> {
    const res = await fetchWithCsrf("/api/vocabulary", {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ lemma, sourceLanguage, targetLanguage }),
    });

    if (!res.ok) {
        throw new Error(`Deleting vocabulary status failed: ${res.status}`);
    }
}