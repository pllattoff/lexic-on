import { useLayoutEffect, useRef, useState, type MouseEvent, type SubmitEvent } from "react";
import CircleButton from "./CircleButton.tsx";
import ConfirmDialog from "./ConfirmDialog.tsx";
import LanguageSelector from "./LanguageSelector.tsx";
import LoadingDots from "./LoadingDots.tsx";
import PlusIcon from "./icons/PlusIcon.tsx";
import RenderedText from "./RenderedText.tsx";
import VocabularyStatsBar from "./VocabularyStatsBar.tsx";
import WordPopup from "./WordPopup.tsx";
import { processText } from "../api/textApi.ts";
import { assignStatusToLemmas, deleteVocabularyStatus, saveVocabularyStatus } from "../api/vocabularyApi.ts";
import { useStatusRequest } from "../hooks/useStatusRequest.ts";
import type { TextSession } from "../hooks/useTextSession.ts";
import { getUntrackedLemmas } from "../lib/vocabulary.ts";
import type { WordsStatusChangeHandler } from "../types/dictionary.ts";
import type { ProcessedText, TextToken, VocabularyStatus } from "../types/text.ts";

type WordPopupState = {
    lemma: string;
    translation: string;
    top: number;
    left: number;
};

const FIELD_CLASSNAME = "min-h-40 w-full rounded-lg border p-4 text-body";
const EDIT_FIELD_CLASSNAME = `${FIELD_CLASSNAME} border-outline bg-surface-2`;
const READ_FIELD_CLASSNAME = `${FIELD_CLASSNAME} border-transparent bg-panel`;

const BUTTON_CLASSNAME =
    "self-end w-32 rounded-md px-4 py-2 text-center font-medium text-heading";

// All occurrences of a lemma share the same status, so the first match is enough
function findLemmaStatus(processedText: ProcessedText | null, lemma: string): VocabularyStatus | null {
    return processedText?.tokens.find((token) => token.lemma === lemma)?.status ?? null;
}

type TextPageProps = {
    isAuthenticated: boolean;
    textSession: TextSession;
    onWordsStatusChange: WordsStatusChangeHandler;
};

export default function TextPage({ isAuthenticated, textSession, onWordsStatusChange }: Readonly<TextPageProps>) {
    const {
        inputText,
        setInputText,
        sourceLanguage,
        setSourceLanguage,
        targetLanguage,
        setTargetLanguage,
        processedText,
        setProcessedText,
        hiddenHighlights,
        toggleHighlight,
        clearText,
    } = textSession;
    const [loading, setLoading] = useState(false);
    const [wordPopupState, setWordPopupState] = useState<WordPopupState | null>(null);
    const [clearConfirmOpen, setClearConfirmOpen] = useState(false);
    const { pending: statusPending, errorMessage: statusError, runStatusRequest, clearError: clearStatusError } = useStatusRequest();
    // A separate state for adding all untracked words, so its loading and error do not show up in the word popup
    const {
        pending: bulkPending,
        errorMessage: bulkError,
        runStatusRequest: runBulkStatusRequest,
    } = useStatusRequest("Couldn't add the words. Try again.");
    // Store a reference to the textarea DOM element for direct DOM manipulation
    const textareaRef = useRef<HTMLTextAreaElement | null>(null);

    const isEditing = processedText === null;

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

    // Add every untracked word of the text to the vocabulary with one status; returns whether it succeeded
    async function handleAddAllUntracked(status: VocabularyStatus): Promise<boolean> {
        if (!processedText) return false;

        const untrackedLemmas = getUntrackedLemmas(processedText.tokens);

        return await runBulkStatusRequest(async () => {
            const addedLemmas = await assignStatusToLemmas(untrackedLemmas, sourceLanguage, targetLanguage, status);
            onWordsStatusChange(sourceLanguage, targetLanguage, addedLemmas, status);
        });
    }

    function handleClearConfirm() {
        setClearConfirmOpen(false);
        clearText();
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
        clearStatusError();

        // The first click on a word starts tracking it: clicking means it was not known well enough to skip
        if (isAuthenticated && token.status === null) {
            await handleStatusChange(token.lemma, "UNKNOWN");
        }
    }

    // Update the status on the backend first, then update it in the text and the dictionaries
    async function handleStatusChange(lemma: string, status: VocabularyStatus) {
        await runStatusRequest(async () => {
            const savedStatus = await saveVocabularyStatus(lemma, sourceLanguage, targetLanguage, status);
            onWordsStatusChange(sourceLanguage, targetLanguage, [lemma], savedStatus);
        });
    }

    async function handleStatusDelete(lemma: string) {
        await runStatusRequest(async () => {
            await deleteVocabularyStatus(lemma, sourceLanguage, targetLanguage);
            onWordsStatusChange(sourceLanguage, targetLanguage, [lemma], null);
        });
    }

    return (
        <div className="mx-auto max-w-3xl p-4">
            <form onSubmit={handleSubmit} className="flex flex-col items-start gap-3">
                {/* The language selector stays centered; the clear button sits at the right edge of the row */}
                <div className="relative flex w-full justify-center">
                    <LanguageSelector
                        isEditing={isEditing}
                        sourceLanguage={sourceLanguage}
                        onSourceLanguageChange={setSourceLanguage}
                        targetLanguage={targetLanguage}
                        onTargetLanguageChange={setTargetLanguage}
                    />

                    {inputText && (
                        <CircleButton
                            label="Start a new text"
                            className="absolute right-1 top-1/2 -translate-y-1/2"
                            onClick={() => setClearConfirmOpen(true)}
                        >
                            <PlusIcon />
                        </CircleButton>
                    )}
                </div>

                {!isEditing && (
                    <VocabularyStatsBar
                        tokens={processedText.tokens}
                        hiddenHighlights={hiddenHighlights}
                        isAuthenticated={isAuthenticated}
                        addAllPending={bulkPending}
                        addAllError={bulkError}
                        onHighlightToggle={toggleHighlight}
                        onAddAllUntracked={handleAddAllUntracked}
                    />
                )}

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
                        hiddenHighlights={hiddenHighlights}
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

            {clearConfirmOpen && (
                <ConfirmDialog
                    title="Start a new text?"
                    message="The current text will be cleared."
                    confirmLabel="Clear"
                    onConfirm={handleClearConfirm}
                    onCancel={() => setClearConfirmOpen(false)}
                />
            )}

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

