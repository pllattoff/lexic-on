import type { MouseEvent, ReactNode } from "react";
import type { HighlightKey } from "../../lib/vocabulary.ts";
import { STATUS_STYLES, UNTRACKED_STYLES } from "../../lib/vocabularyStyles.ts";
import type { ProcessedText, TextToken } from "../../types/text.ts";
import type { VocabularyStatus } from "../../types/vocabulary.ts";

// Return the highlight styles for a word based on its status and whether that status is hidden in the stats bar
function getWordHighlightClassName(status: VocabularyStatus | null, hiddenHighlights: ReadonlySet<HighlightKey>): string {
    if (hiddenHighlights.has(status ?? "UNTRACKED")) return UNTRACKED_STYLES.highlightOff;
    if (status === null) return UNTRACKED_STYLES.highlight;

    return `${STATUS_STYLES[status].highlight} ${STATUS_STYLES[status].highlightHover}`;
}

type RenderedTextProps = {
    processedText: ProcessedText;
    hiddenHighlights: ReadonlySet<HighlightKey>;
    className: string;
    onWordClick: (event: MouseEvent<HTMLSpanElement>, token: TextToken) => void;
};

export default function RenderedText({
                                         processedText,
                                         hiddenHighlights,
                                         className,
                                         onWordClick,
                                     }: Readonly<RenderedTextProps>) {
    const { text, tokens } = processedText;
    const textParts: ReactNode[] = [];
    let cursor = 0;

    // Build interactive text from plain text and clickable words
    tokens.forEach((token) => {
        // Add the text between the previous token and the current one
        if (token.start > cursor) {
            textParts.push(text.slice(cursor, token.start));
        }

        const wordHighlightClassName = getWordHighlightClassName(token.status, hiddenHighlights);

        // Add the token as a clickable span, highlighted if the word is in the user's vocabulary
        textParts.push(
            <span
                key={`${token.start}-${token.end}`}
                onClick={(e) => onWordClick(e, token)}
                className={`-mx-[1px] cursor-pointer rounded-sm px-[1px] ${wordHighlightClassName}`}
            >
                {text.slice(token.start, token.end)}
            </span>,
        );

        cursor = token.end;
    });

    // Add any remaining text after the last token
    if (cursor < text.length) {
        textParts.push(text.slice(cursor));
    }

    return (
        <p className={className} style={{ whiteSpace: "pre-wrap" }}>
            {textParts}
        </p>
    );
}
