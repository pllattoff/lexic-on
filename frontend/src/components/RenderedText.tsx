import type { MouseEvent, ReactNode } from "react";
import { STATUS_STYLES, UNTRACKED_STYLES, type HighlightKey } from "../lib/vocabulary.ts";
import type { ProcessedText, TextToken, VocabularyStatus } from "../types/text.ts";

// Determine the CSS classes of a word based on its vocabulary status and the highlights switched on in the stats bar
function wordClassName(status: VocabularyStatus | null, hiddenHighlights: ReadonlySet<HighlightKey>): string {
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

        const statusClassName = wordClassName(token.status, hiddenHighlights);

        // Add the token as a clickable span, highlighted if the word is in the user's vocabulary
        textParts.push(
            <span
                key={`${token.start}-${token.end}`}
                onClick={(e) => onWordClick(e, token)}
                className={`-mx-[1px] cursor-pointer rounded-sm px-[1px] ${statusClassName}`}
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
