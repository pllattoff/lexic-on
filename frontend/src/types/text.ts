export type VocabularyStatus = "UNKNOWN" | "REVIEW" | "KNOWN";

export type TextToken = {
    start: number;
    end: number;
    lemma: string;
    translation: string;
    // null if the word is untracked (never clicked) or the user is a guest
    status: VocabularyStatus | null;
};

export type ProcessedText = {
    text: string;
    tokens: TextToken[];
};