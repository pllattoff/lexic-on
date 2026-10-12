import type { TextToken, VocabularyStatus } from "../types/text.ts";

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

export const STATUS_LABELS: Record<VocabularyStatus, string> = {
    UNKNOWN: "Unknown",
    REVIEW: "Review",
    KNOWN: "Known",
};

// Shape of the status tiles: the stats bar, the cards on the Dictionaries page and the tabs of a dictionary
export const STATUS_TILE_CLASSNAME = "rounded-md border px-3 py-2 text-center";

// Tailwind finds classes only in complete strings, so they are written out for every status instead of being built from a color name
type StatusStyle = {
    // Words in the text
    highlight: string;
    highlightHover: string;
    // Colored blocks: the stats bar and the cards on the Dictionaries page
    block: string;
    blockHover: string;
    // Status buttons: the word popup and the rows of a dictionary
    segment: string;
    segmentSelected: string;
    // Status links in the sidebar
    nav: string;
    navSelected: string;
    // Status tabs on a dictionary page
    tab: string;
    tabSelected: string;
};

// Words without a status are not a vocabulary status, so their styles are kept next to STATUS_STYLES
export const UNTRACKED_STYLES = {
    // Words in the text: highlighted, a bit dimmer than the hover color of the plain ones, and lighter on hover
    highlight: "bg-outline/40 hover:bg-outline/60 hover:text-body",
    // Words in the text with the highlight switched off: only the hover color, a bit lighter than the tile
    highlightOff: "hover:bg-outline/50 hover:text-body",
    // The stats bar tile. Its border color is inherited by the divider between the two parts of the tile,
    // and the hover colors are separate, because each part of the tile is highlighted on its own
    tile: "border-outline-strong bg-surface-2 text-body",
    tileHover: "hover:bg-outline/40",
    // The tile with the highlight switched off, dimmed like the status tabs of a dictionary
    tileDimmed: "border-outline/40 bg-surface-2/50 text-body/50",
    tileDimmedHover: "hover:bg-outline/30 hover:text-body/80",
};

export const STATUS_STYLES: Record<VocabularyStatus, StatusStyle> = {
    UNKNOWN: {
        highlight: "bg-unknown/20",
        highlightHover: "hover:bg-unknown/15 hover:text-unknown/80",
        block: "border-unknown/40 bg-unknown/15 text-unknown",
        blockHover: "hover:border-unknown hover:bg-unknown/25 hover:shadow-md hover:shadow-unknown/20",
        segment: "text-unknown/60 hover:text-unknown",
        segmentSelected: "text-unknown scale-105",
        nav: "border-unknown/60 text-body hover:border-unknown hover:text-heading",
        navSelected: "border-unknown bg-unknown/10 text-heading",
        tab: "border-unknown/30 bg-unknown/10 text-unknown/50 hover:bg-unknown/15 hover:text-unknown/80",
        tabSelected: "border-unknown bg-unknown/25 text-unknown shadow-md shadow-unknown/20",
    },
    REVIEW: {
        highlight: "bg-review/20",
        highlightHover: "hover:bg-review/15 hover:text-review/80",
        block: "border-review/40 bg-review/15 text-review",
        blockHover: "hover:border-review hover:bg-review/25 hover:shadow-md hover:shadow-review/20",
        segment: "text-review/60 hover:text-review",
        segmentSelected: "text-review scale-105",
        nav: "border-review/60 text-body hover:border-review hover:text-heading",
        navSelected: "border-review bg-review/10 text-heading",
        tab: "border-review/30 bg-review/10 text-review/50 hover:bg-review/15 hover:text-review/80",
        tabSelected: "border-review bg-review/25 text-review shadow-md shadow-review/20",
    },
    KNOWN: {
        highlight: "bg-known/20",
        highlightHover: "hover:bg-known/15 hover:text-known/80",
        block: "border-known/40 bg-known/15 text-known",
        blockHover: "hover:border-known hover:bg-known/25 hover:shadow-md hover:shadow-known/20",
        segment: "text-known/60 hover:text-known",
        segmentSelected: "text-known scale-105",
        nav: "border-known/60 text-body hover:border-known hover:text-heading",
        navSelected: "border-known bg-known/10 text-heading",
        tab: "border-known/30 bg-known/10 text-known/50 hover:bg-known/15 hover:text-known/80",
        tabSelected: "border-known bg-known/25 text-known shadow-md shadow-known/20",
    },
};