import { fetchWithCsrf } from "../lib/csrf.ts";
import type { ProcessedText } from "../types/text.ts";
import type { SourceLanguage, TargetLanguage } from "../lib/languages.ts";

export async function processText(
    text: string,
    sourceLanguage: SourceLanguage,
    targetLanguage: TargetLanguage,
): Promise<ProcessedText> {
    const res = await fetchWithCsrf("/api/text/process", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text, sourceLanguage, targetLanguage }),
    });

    if (!res.ok) {
        throw new Error(`Text processing failed: ${res.status}`);
    }

    return res.json();
}