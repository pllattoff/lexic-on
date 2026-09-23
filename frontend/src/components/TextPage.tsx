import { useState, type ReactNode, type SubmitEvent } from "react";
import { fetchWithCsrf } from "../lib/csrf";

type TextToken = {
    start: number;
    end: number;
    lemma: string;
};

type ProcessedText = {
    text: string;
    tokens: TextToken[];
};

export default function TextPage() {
    const [input, setInput] = useState("");
    const [processedText, setProcessedText] = useState<ProcessedText | null>(null);
    const [loading, setLoading] = useState(false);

    async function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
        e.preventDefault();
        setLoading(true);

        try {
            const res = await fetchWithCsrf("/api/text/process", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ text: input }),
            });

            setProcessedText(res.ok ? await res.json() : null);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div>
            <form onSubmit={handleSubmit}>
                <textarea
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    rows={6}
                />
                <button
                    type="submit"
                    disabled={loading || !input.trim()}
                >
                    Process
                </button>
            </form>

            {processedText && <RenderedText processedText={processedText} />}
        </div>
    );
}

function RenderedText({ processedText }: { processedText: ProcessedText }) {
    const { text, tokens } = processedText;
    const parts: ReactNode[] = [];
    let cursor = 0;

    // Build the rendered text from plain text and clickable token spans
    tokens.forEach((token) => {
        // Add the text between the previous token and the current one
        if (token.start > cursor) {
            parts.push(text.slice(cursor, token.start));
        }

        // Add the token as a clickable span
        parts.push(
            <span
                key={`${token.start}-${token.end}`}
                onClick={() => console.log(token.lemma)}
                style={{ cursor: "pointer" }}
            >
                {text.slice(token.start, token.end)}
            </span>,
        );

        cursor = token.end;
    });

    // Add any remaining text after the last token
    if (cursor < text.length) {
        parts.push(text.slice(cursor));
    }

    return (
        <p style={{ whiteSpace: "pre-wrap" }}>
            {parts}
        </p>
    );
}