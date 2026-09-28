export type TextToken = {
    start: number;
    end: number;
    lemma: string;
};

export type ProcessedText = {
    text: string;
    tokens: TextToken[];
};