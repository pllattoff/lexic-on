export type SourceLanguage = "en";
export type TargetLanguage = "de" | "uk";

export const SOURCE_LANGUAGES: readonly SourceLanguage[] = ["en"];
export const TARGET_LANGUAGES: readonly TargetLanguage[] = ["de", "uk"];

// Map every language code to its display label.
// Record requires every type value to have an assigned value.
export const LANGUAGE_LABELS: Record<SourceLanguage | TargetLanguage, string> = {
    en: "English",
    de: "Deutsch",
    uk: "Українська",
};