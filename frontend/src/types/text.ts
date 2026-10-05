export type TextToken = {
    start: number;
    end: number;
    lemma: string;
    translation: string;
};

export type ProcessedText = {
    text: string;
    tokens: TextToken[];
};