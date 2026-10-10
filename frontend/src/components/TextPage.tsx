import { useEffect, useLayoutEffect, useRef, useState, type MouseEvent, type ReactNode, type SubmitEvent } from "react";
import LoadingDots from "./LoadingDots.tsx";
import VocabularyStatsBar from "./VocabularyStatsBar.tsx";
import WordPopup from "./WordPopup.tsx";
import { processText } from "../api/textApi.ts";
import { deleteVocabularyStatus, saveVocabularyStatus } from "../api/vocabularyApi.ts";
import { INPUT_TEXT_STORAGE_KEY } from "../constants/storage.ts";
import { LANGUAGE_LABELS, SOURCE_LANGUAGES, TARGET_LANGUAGES, type SourceLanguage, type TargetLanguage } from "../lib/languages.ts";
import { STATUS_STYLES } from "../lib/vocabulary.ts";
import type { ProcessedText, TextToken, VocabularyStatus } from "../types/text.ts";

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

// Hover colors for words without a status, a bit lighter than the Untracked block in the stats bar
const UNTRACKED_HOVER_CLASSNAME = "hover:bg-outline/50 hover:text-body";

// Shared layout for both editing and read-only language selectors
const LANGUAGE_ROW_CLASSNAME = "flex h-10 items-center gap-2 self-center";

// All occurrences of a lemma share the same status, so the first match is enough
function findLemmaStatus(processedText: ProcessedText | null, lemma: string): VocabularyStatus | null {
    return processedText?.tokens.find((token) => token.lemma === lemma)?.status ?? null;
}

export default function TextPage({ isAuthenticated }: Readonly<{ isAuthenticated: boolean }>) {
    const [inputText, setInputText] = useState(
        () => sessionStorage.getItem(INPUT_TEXT_STORAGE_KEY) ?? "",
    );
    const [sourceLanguage, setSourceLanguage] = useState<SourceLanguage>("en");
    const [targetLanguage, setTargetLanguage] = useState<TargetLanguage>("de");
    const [processedText, setProcessedText] = useState<ProcessedText | null>(null);
    const [loading, setLoading] = useState(false);
    const [wordPopupState, setWordPopupState] = useState<WordPopupState | null>(null);
    const [statusPending, setStatusPending] = useState(false);
    const [statusError, setStatusError] = useState<string | null>(null);
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

    async function handleWordClick(event: MouseEvent<HTMLSpanElement>, token: TextToken) {
        const rect = event.currentTarget.getBoundingClientRect();
        setWordPopupState({
            lemma: token.lemma,
            translation: token.translation,
            top: rect.bottom + 6,
            left: rect.left,
        });
        setStatusError(null);

        // The first click on a word starts tracking it: clicking means it was not known well enough to skip
        if (isAuthenticated && token.status === null) {
            await handleStatusChange(token.lemma, "UNKNOWN");
        }
    }

    // Update the status on the backend first, then update the status in the local processedText state
    async function handleStatusChange(lemma: string, status: VocabularyStatus) {
        await runStatusRequest(async () => {
            const savedStatus = await saveVocabularyStatus(lemma, sourceLanguage, targetLanguage, status);
            updateLemmaStatus(lemma, savedStatus);
        });
    }

    async function handleStatusDelete(lemma: string) {
        await runStatusRequest(async () => {
            await deleteVocabularyStatus(lemma, sourceLanguage, targetLanguage);
            updateLemmaStatus(lemma, null);
        });
    }

    // Handle status requests, ensuring the UI is updated only after the backend confirms the change
    async function runStatusRequest(request: () => Promise<void>) {
        setStatusPending(true);
        setStatusError(null);

        try {
            await request();
        } catch {
            setStatusError("Couldn't save the status. Try again.");
        } finally {
            setStatusPending(false);
        }
    }

    // Update all occurrences of the lemma in processedText state without processing the text again
    function updateLemmaStatus(lemma: string, status: VocabularyStatus | null) {
        setProcessedText((current) => {
            if (!current) return current;

            const tokens = current.tokens.map((token) => {
                if (token.lemma !== lemma) return token;

                // Copy the token and override its status property with the new value
                return { ...token, status };
            });

            return { ...current, tokens };
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

                {!isEditing && <VocabularyStatsBar tokens={processedText.tokens} />}

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
                        <span className="relative">
                            Process
                            {loading && <LoadingDots />}
                        </span>
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
                    status={findLemmaStatus(processedText, wordPopupState.lemma)}
                    isAuthenticated={isAuthenticated}
                    pending={statusPending}
                    errorMessage={statusError}
                    top={wordPopupState.top}
                    left={wordPopupState.left}
                    onStatusChange={(status) => handleStatusChange(wordPopupState.lemma, status)}
                    onDelete={() => handleStatusDelete(wordPopupState.lemma)}
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
    readonly onWordClick: (event: MouseEvent<HTMLSpanElement>, token: TextToken) => void;
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

        // Determine the CSS classes based on the vocabulary status
        const statusClassName = token.status
            ? `${STATUS_STYLES[token.status].highlight} ${STATUS_STYLES[token.status].highlightHover}`
            : UNTRACKED_HOVER_CLASSNAME;

        // Add the token as a clickable span, highlighted if the word is in the user's vocabulary
        textParts.push(
            <span
                key={`${token.start}-${token.end}`}
                onClick={(e) => onWordClick(e, token)}
                className={`-mx-[1px] cursor-pointer rounded-sm px-[1px] ${statusClassName}`}
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