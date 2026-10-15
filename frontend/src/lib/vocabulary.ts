import type { TextToken } from "../types/text.ts";
import type { VocabularyStatus } from "../types/vocabulary.ts";

// Statuses are ordered from unknown towards known
export const VOCABULARY_STATUSES: readonly VocabularyStatus[] = ["UNKNOWN", "REVIEW", "KNOWN"];

// Returns unique lemmas that have no vocabulary status yet.
export function getUntrackedLemmas(tokens: readonly TextToken[]): string[] {
    return [...new Set(tokens.filter((token) => token.status === null).map((token) => token.lemma))];
}

// What the stats bar tiles can switch on and off in the text: every status plus the words without one
export type HighlightKey = VocabularyStatus | "UNTRACKED";

// A new text starts with the untracked words not highlighted
export const DEFAULT_HIDDEN_HIGHLIGHTS: readonly HighlightKey[] = ["UNTRACKED"];

// Translation keys of the status names; translate them with t() where they are shown
export const STATUS_LABEL_KEYS: Record<VocabularyStatus, string> = {
    UNKNOWN: "vocabulary.status.unknown",
    REVIEW: "vocabulary.status.review",
    KNOWN: "vocabulary.status.known",
};
