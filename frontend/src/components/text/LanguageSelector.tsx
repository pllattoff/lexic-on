import { LANGUAGE_LABELS, SOURCE_LANGUAGES, TARGET_LANGUAGES, type SourceLanguage, type TargetLanguage } from "../../lib/languages.ts";

const SELECT_CLASSNAME =
    "w-32 rounded-md border border-outline bg-surface-2 px-2 py-1 text-body";

// Shared layout for both editing and read-only language selectors
const LANGUAGE_ROW_CLASSNAME = "flex h-10 items-center gap-2 self-center";

type LanguageSelectorProps = {
    isEditing: boolean;
    sourceLanguage: SourceLanguage;
    onSourceLanguageChange: (language: SourceLanguage) => void;
    targetLanguage: TargetLanguage;
    onTargetLanguageChange: (language: TargetLanguage) => void;
};

export default function LanguageSelector({
                                             isEditing,
                                             sourceLanguage,
                                             onSourceLanguageChange,
                                             targetLanguage,
                                             onTargetLanguageChange,
                                         }: Readonly<LanguageSelectorProps>) {
    if (!isEditing) {
        return (
            <div className={`${LANGUAGE_ROW_CLASSNAME} text-body/70`}>
                <span>{LANGUAGE_LABELS[sourceLanguage]}</span>
                <span>→</span>
                <span>{LANGUAGE_LABELS[targetLanguage]}</span>
            </div>
        );
    }

    return (
        <div className={LANGUAGE_ROW_CLASSNAME}>
            <select
                className={SELECT_CLASSNAME}
                value={sourceLanguage}
                onChange={(e) => onSourceLanguageChange(e.target.value as SourceLanguage)}
            >
                {SOURCE_LANGUAGES.map((language) => (
                    <option key={language} value={language}>
                        {LANGUAGE_LABELS[language]}
                    </option>
                ))}
            </select>

            <span className="text-body">→</span>

            <select
                className={SELECT_CLASSNAME}
                value={targetLanguage}
                onChange={(e) => onTargetLanguageChange(e.target.value as TargetLanguage)}
            >
                {TARGET_LANGUAGES.map((language) => (
                    <option key={language} value={language}>
                        {LANGUAGE_LABELS[language]}
                    </option>
                ))}
            </select>
        </div>
    );
}
