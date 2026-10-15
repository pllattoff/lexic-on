import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import { DEFAULT_UI_LANGUAGE, detectUiLanguage } from "./lib/uiLanguages.ts";
import de from "./locales/de.json";
import en from "./locales/en.json";
import uk from "./locales/uk.json";

const initialLanguage = detectUiLanguage();

document.documentElement.lang = initialLanguage;

// Update the page's HTML language when the selected UI language changes
i18n.on("languageChanged", (language) => {
    document.documentElement.lang = language;
});

void i18n.use(initReactI18next).init({
    resources: {
        en: { translation: en },
        de: { translation: de },
        uk: { translation: uk },
    },
    lng: initialLanguage,
    fallbackLng: DEFAULT_UI_LANGUAGE,
    interpolation: { escapeValue: false }, // React already escapes the values
});

export default i18n;