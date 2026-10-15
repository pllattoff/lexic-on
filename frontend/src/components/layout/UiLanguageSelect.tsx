import { useTranslation } from "react-i18next";
import { LANGUAGE_LABELS } from "../../lib/languages.ts";
import { isUiLanguage, saveUiLanguage, UI_LANGUAGE_SHORT_LABELS, UI_LANGUAGES } from "../../lib/uiLanguages.ts";
import GlobeIcon from "../icons/GlobeIcon.tsx";

// Switches the interface language; the choice is remembered between visits
export default function UiLanguageSelect() {
    const { t, i18n } = useTranslation();

    function handleChange(code: string) {
        if (!isUiLanguage(code)) return;

        void i18n.changeLanguage(code);
        saveUiLanguage(code);
    }

    const currentShortLabel = isUiLanguage(i18n.language) ? UI_LANGUAGE_SHORT_LABELS[i18n.language] : null;

    return (
        <label
            className="relative flex w-fit items-center gap-2 rounded-md px-3 py-1.5 text-xs text-body hover:text-heading has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-accent">
            <GlobeIcon />
            <span>{t("sidebar.language")}:</span>

            {/* The visible value shows the short code of the current language */}
            <span className="flex items-center gap-1">
                {currentShortLabel}
                <span aria-hidden="true">▾</span>
            </span>

            {/* The real select is transparent and covers the whole control: it receives the clicks
                and opens the native list with the full language names */}
            <select
                value={i18n.language}
                onChange={(e) => handleChange(e.target.value)}
                className="absolute inset-0 h-full w-full cursor-pointer bg-surface-2 text-body opacity-0"
            >
                {UI_LANGUAGES.map((language) => (
                    <option key={language} value={language}>
                        {LANGUAGE_LABELS[language]}
                    </option>
                ))}
            </select>
        </label>
    );
}
