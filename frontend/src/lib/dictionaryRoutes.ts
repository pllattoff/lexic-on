import { SOURCE_LANGUAGES, TARGET_LANGUAGES, type SourceLanguage, type TargetLanguage } from "./languages.ts";
import { VOCABULARY_STATUSES } from "./vocabulary.ts";
import type { VocabularyStatus } from "../types/vocabulary.ts";

// Dictionary routes: /dictionaries/{sourceLanguage}-{targetLanguage}
export function dictionaryPath(sourceLanguage: SourceLanguage, targetLanguage: TargetLanguage): string {
    return `/dictionaries/${sourceLanguage}-${targetLanguage}`;
}

// Dictionary status routes: /dictionaries/{sourceLanguage}-{targetLanguage}/{status}
export function dictionaryStatusPath(
    sourceLanguage: SourceLanguage,
    targetLanguage: TargetLanguage,
    status: VocabularyStatus,
): string {
    return `${dictionaryPath(sourceLanguage, targetLanguage)}/${status.toLowerCase()}`;
}

// Parses a language pair string into the corresponding language codes
export function parseLanguagePair(
    pair: string | undefined,
): { sourceLanguage: SourceLanguage; targetLanguage: TargetLanguage } | null {
    // Collects any elements beyond the first two into the rest array
    const [sourceCode, targetCode, ...rest] = (pair ?? "").split("-");
    const sourceLanguage = SOURCE_LANGUAGES.find((language) => language === sourceCode);
    const targetLanguage = TARGET_LANGUAGES.find((language) => language === targetCode);

    if (!sourceLanguage || !targetLanguage || rest.length > 0) return null;

    return { sourceLanguage, targetLanguage };
}

export function parseStatus(statusSlug: string | undefined): VocabularyStatus | null {
    return VOCABULARY_STATUSES.find((status) => status.toLowerCase() === statusSlug) ?? null;
}