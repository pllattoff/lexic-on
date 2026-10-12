import type { SourceLanguage, TargetLanguage } from "../lib/languages.ts";
import type { VocabularyStatus } from "./vocabulary.ts";

export type DictionarySummary = {
    sourceLanguage: SourceLanguage;
    targetLanguage: TargetLanguage;
    wordCounts: Record<VocabularyStatus, number>;
};

// Called after words of a language pair got a new status; null as the status means they were removed from the vocabulary
export type WordsStatusChangeHandler = (
    sourceLanguage: SourceLanguage,
    targetLanguage: TargetLanguage,
    lemmas: readonly string[],
    status: VocabularyStatus | null,
) => void;

export type DictionaryWord = {
    lemma: string;
    translation: string;
};