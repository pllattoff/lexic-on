import { UI_LANGUAGE_STORAGE_KEY } from "./storage.ts";

export type UiLanguage = "en" | "de" | "uk";

export const UI_LANGUAGES: readonly UiLanguage[] = ["en", "de", "uk"];
export const DEFAULT_UI_LANGUAGE: UiLanguage = "en";

// Short visual labels of the closed language selector. Ukrainian is shown as "UA" because it is
// the familiar abbreviation, while "uk" stays the language code used inside the app.
export const UI_LANGUAGE_SHORT_LABELS: Record<UiLanguage, string> = {
    en: "EN",
    de: "DE",
    uk: "UA",
};

export function isUiLanguage(code: string): code is UiLanguage {
    return (UI_LANGUAGES as readonly string[]).includes(code);
}

// Saved choice first, then the browser languages in their order of preference, then the default
export function detectUiLanguage(): UiLanguage {
    const savedLanguage = readSavedUiLanguage();
    if (savedLanguage) return savedLanguage;

    // navigator.languages can be empty in some browsers, then only navigator.language is available
    const browserLanguages = navigator.languages.length > 0 ? navigator.languages : [navigator.language];

    for (const browserLanguage of browserLanguages) {
        // "de-DE" -> "de"
        const baseLanguage = browserLanguage.split("-")[0].toLowerCase();
        if (isUiLanguage(baseLanguage)) return baseLanguage;
    }

    return DEFAULT_UI_LANGUAGE;
}

export function saveUiLanguage(language: UiLanguage) {
    try {
        localStorage.setItem(UI_LANGUAGE_STORAGE_KEY, language);
    } catch {
        // Storage can be blocked, the language then applies only until the page is reloaded
    }
}

function readSavedUiLanguage(): UiLanguage | null {
    try {
        const saved = localStorage.getItem(UI_LANGUAGE_STORAGE_KEY);
        return saved !== null && isUiLanguage(saved) ? saved : null;
    } catch {
        return null; // Storage can be blocked
    }
}
