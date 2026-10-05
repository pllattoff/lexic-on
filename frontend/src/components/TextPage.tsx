import { useEffect, useLayoutEffect, useRef, useState, type MouseEvent, type ReactNode, type SubmitEvent } from "react";
import WordPopup from "./WordPopup.tsx";
import type {ProcessedText} from "../types/text.ts";
import {processText} from "../api/textApi.ts";
import {INPUT_TEXT_STORAGE_KEY} from "../constants/storage.ts";
import { LANGUAGE_LABELS, SOURCE_LANGUAGES, TARGET_LANGUAGES, type SourceLanguage, type TargetLanguage } from "../lib/languages.ts";

type WordPopupState = {
    lemma: string;
    translation: string;
    top: number;
    left: number;
};

const FIELD_CLASSNAME = "min-h-40 w-full rounded-lg border p-4 text-body";
const EDIT_FIELD_CLASSNAME = `${FIELD_CLASSNAME} border-outline bg-surface-2`;
const READ_FIELD_CLASSNAME = `${FIELD_CLASSNAME} border-transparent bg-surface/90`;

const BUTTON_CLASSNAME =
    "self-end w-32 rounded-md px-4 py-2 text-center font-medium text-heading";

const SELECT_CLASSNAME =
    "w-32 rounded-md border border-outline bg-surface-2 px-2 py-1 text-body";

// Shared layout for both editing and read-only language selectors
const LANGUAGE_ROW_CLASSNAME = "flex h-10 items-center gap-2 self-center";

export default function TextPage() {
    const [inputText, setInputText] = useState(
        () => sessionStorage.getItem(INPUT_TEXT_STORAGE_KEY) ?? "",
    );
    const [sourceLanguage, setSourceLanguage] = useState<SourceLanguage>("en");
    const [targetLanguage, setTargetLanguage] = useState<TargetLanguage>("de");
    const [processedText, setProcessedText] = useState<ProcessedText | null>(null);
    const [loading, setLoading] = useState(false);
    const [wordPopupState, setWordPopupState] = useState<WordPopupState | null>(null);
    // Store a reference to the textarea DOM element for direct DOM manipulation
    const textareaRef = useRef<HTMLTextAreaElement | null>(null);

    const isEditing = processedText === null;

    // Save the input text to sessionStorage for recovery after page reloads or GitHub login redirect
    useEffect(() => {
        sessionStorage.setItem(INPUT_TEXT_STORAGE_KEY, inputText);
    }, [inputText]);

    // Grow the textarea to fit its content instead of a fixed row count.
    // Also re-runs on isEditing so it fits existing text right when the textarea (re)mounts,
    // not just on the next keystroke.
    // (useLayoutEffect - runs after the DOM is updated but before the browser paints)
    useLayoutEffect(() => {
        const textarea = textareaRef.current;
        if (!textarea) return;

        textarea.style.height = "auto";
        textarea.style.height = `${textarea.scrollHeight}px`;
    }, [inputText, isEditing]);

    async function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
        e.preventDefault();
        setLoading(true);

        try {
            const processedText = await processText(inputText, sourceLanguage, targetLanguage);
            setProcessedText(processedText);
        } finally {
            setLoading(false);
        }
    }

    function handleEdit() {
        setProcessedText(null);
    }

    function handleWordClick(event: MouseEvent<HTMLSpanElement>, lemma: string, translation: string) {
        const rect = event.currentTarget.getBoundingClientRect();
        setWordPopupState({
            lemma,
            translation,
            top: rect.bottom + 6,
            left: rect.left,
        });
    }

    return (
        <div className="mx-auto max-w-3xl p-4">
            <form onSubmit={handleSubmit} className="flex flex-col items-start gap-3">
                <LanguageSelector
                    isEditing={isEditing}
                    sourceLanguage={sourceLanguage}
                    onSourceLanguageChange={setSourceLanguage}
                    targetLanguage={targetLanguage}
                    onTargetLanguageChange={setTargetLanguage}
                />

                {isEditing ? (
                    <textarea
                        ref={textareaRef}   // Assign the textarea DOM element to textareaRef.current
                        value={inputText}
                        onChange={(e) => setInputText(e.target.value)}
                        placeholder="Paste or type your text here..."
                        className={`${EDIT_FIELD_CLASSNAME} resize-none overflow-hidden placeholder:text-body/50 focus:border-outline-strong focus:outline-none`}
                    />
                ) : (
                    <RenderedText
                        processedText={processedText}
                        className={READ_FIELD_CLASSNAME}
                        onWordClick={handleWordClick}
                    />
                )}

                {isEditing ? (
                    <button
                        key="process"
                        type="submit"    // Submit form
                        disabled={loading || !inputText.trim()}
                        className={`${BUTTON_CLASSNAME} border border-known/40 bg-known/20 hover:bg-known/30 disabled:cursor-not-allowed disabled:opacity-40`}
                    >
                        Process
                    </button>
                ) : (
                    <button
                        key="edit"
                        type="button"    // Handle click only
                        onClick={handleEdit}
                        className={`${BUTTON_CLASSNAME} border border-accent/40 bg-accent/20 hover:bg-accent/30`}
                    >
                        Edit
                    </button>
                )}
            </form>

            {wordPopupState && (
                <WordPopup
                    lemma={wordPopupState.lemma}
                    translation={wordPopupState.translation}
                    top={wordPopupState.top}
                    left={wordPopupState.left}
                    onClose={() => setWordPopupState(null)}
                />
            )}
        </div>
    );
}

function LanguageSelector({
                              isEditing,
                              sourceLanguage,
                              onSourceLanguageChange,
                              targetLanguage,
                              onTargetLanguageChange,
                          }: {
    readonly isEditing: boolean;
    readonly sourceLanguage: SourceLanguage;
    readonly onSourceLanguageChange: (language: SourceLanguage) => void;
    readonly targetLanguage: TargetLanguage;
    readonly onTargetLanguageChange: (language: TargetLanguage) => void;
}) {
    if (!isEditing) {
        return (
            <div className={`${LANGUAGE_ROW_CLASSNAME} text-body`}>
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

function RenderedText({
                          processedText,
                          className,
                          onWordClick,
                      }: {
    readonly processedText: ProcessedText;
    readonly className: string;
    readonly onWordClick: (event: MouseEvent<HTMLSpanElement>, lemma: string, translation: string) => void;
}) {
    const { text, tokens } = processedText;
    const textParts: ReactNode[] = [];
    let cursor = 0;

    // Build interactive text from plain text and clickable words
    tokens.forEach((token) => {
        // Add the text between the previous token and the current one
        if (token.start > cursor) {
            textParts.push(text.slice(cursor, token.start));
        }

        // Add the token as a clickable span
        textParts.push(
            <span
                key={`${token.start}-${token.end}`}
                onClick={(e) => onWordClick(e, token.lemma, token.translation)}
                className="cursor-pointer hover:underline"
            >
                 {text.slice(token.start, token.end)}
             </span>,
        );

        cursor = token.end;
    });

    // Add any remaining text after the last token
    if (cursor < text.length) {
        textParts.push(text.slice(cursor));
    }

    return (
        <p className={className} style={{ whiteSpace: "pre-wrap" }}>
            {textParts}
        </p>
    );
}