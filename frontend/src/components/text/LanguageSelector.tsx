import SelectCaret from "../ui/SelectCaret.tsx";
import { LANGUAGE_LABELS, SOURCE_LANGUAGES, TARGET_LANGUAGES, type SourceLanguage, type TargetLanguage } from "../../lib/languages.ts";

// Shared layout for both editing and read-only language selectors
const LANGUAGE_ROW_CLASSNAME = "flex h-10 items-center gap-2 self-center";

type LanguageSelectProps<Language extends SourceLanguage | TargetLanguage> = {
    value: Language;
    languages: readonly Language[];
    onChange: (language: Language) => void;
};

// Looks like the interface language switcher: the visible value with a caret. The real select is transparent
// and covers the whole control, so it receives the clicks and opens the native list.
function LanguageSelect<Language extends SourceLanguage | TargetLanguage>({
                                                                              value,
                                                                              languages,
                                                                              onChange,
                                                                          }: Readonly<LanguageSelectProps<Language>>) {
    return (
        <label
            className="group relative flex w-32 items-center justify-between rounded-md border border-outline bg-surface-2 px-2 py-1 text-body has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-accent">
            <span>{LANGUAGE_LABELS[value]}</span>
            <SelectCaret />

            <select
                value={value}
                onChange={(e) => onChange(e.target.value as Language)}
                className="absolute inset-0 h-full w-full cursor-pointer bg-surface-2 text-body opacity-0"
            >
                {languages.map((language) => (
                    <option key={language} value={language}>
                        {LANGUAGE_LABELS[language]}
                    </option>
                ))}
            </select>
        </label>
    );
}

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
            <LanguageSelect value={sourceLanguage} languages={SOURCE_LANGUAGES} onChange={onSourceLanguageChange} />

            <span className="text-body">→</span>

            <LanguageSelect value={targetLanguage} languages={TARGET_LANGUAGES} onChange={onTargetLanguageChange} />
        </div>
    );
}
