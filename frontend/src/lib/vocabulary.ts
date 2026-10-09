import type { VocabularyStatus } from "../types/text.ts";

// Statuses are ordered from unknown towards known
export const VOCABULARY_STATUSES: readonly VocabularyStatus[] = ["UNKNOWN", "REVIEW", "KNOWN"];

export const STATUS_LABELS: Record<VocabularyStatus, string> = {
    UNKNOWN: "Unknown",
    REVIEW: "Review",
    KNOWN: "Known",
};

export const STATUS_STYLES: Record<VocabularyStatus, { highlight: string; highlightHover: string; block: string; segment: string; segmentSelected: string }> = {
    UNKNOWN: {
        highlight: "bg-unknown/20",
        highlightHover: "hover:bg-unknown/15 hover:text-unknown/80",
        block: "border-unknown/40 bg-unknown/15 text-unknown",
        segment: "text-unknown/60 hover:text-unknown",
        segmentSelected: "text-unknown scale-105",
    },
    REVIEW: {
        highlight: "bg-review/20",
        highlightHover: "hover:bg-review/15 hover:text-review/80",
        block: "border-review/40 bg-review/15 text-review",
        segment: "text-review/60 hover:text-review",
        segmentSelected: "text-review scale-105",
    },
    KNOWN: {
        highlight: "bg-known/20",
        highlightHover: "hover:bg-known/15 hover:text-known/80",
        block: "border-known/40 bg-known/15 text-known",
        segment: "text-known/60 hover:text-known",
        segmentSelected: "text-known scale-105",
    },
};