import { useLayoutEffect, useRef, useState, type MouseEvent, type SubmitEvent } from "react";
import { processText } from "../api/textApi.ts";
import { assignStatusToLemmas, deleteVocabularyStatus, saveVocabularyStatus } from "../api/vocabularyApi.ts";
import PlusIcon from "../components/icons/PlusIcon.tsx";
import LanguageSelector from "../components/text/LanguageSelector.tsx";
import RenderedText from "../components/text/RenderedText.tsx";
import VocabularyStatsBar from "../components/text/VocabularyStatsBar.tsx";
import WordPopup from "../components/text/WordPopup.tsx";
import CircleButton from "../components/ui/CircleButton.tsx";
import ConfirmDialog from "../components/ui/ConfirmDialog.tsx";
import LoadingDots from "../components/ui/LoadingDots.tsx";
import { useAsyncAction } from "../hooks/useAsyncAction.ts";
import type { TextSession } from "../hooks/useTextSession.ts";
import { REQUEST_ERRORS } from "../lib/requestErrors.ts";
import { getUntrackedLemmas } from "../lib/vocabulary.ts";
import type { WordsStatusChangeHandler } from "../types/dictionary.ts";
import type { ProcessedText, TextToken } from "../types/text.ts";
import type { VocabularyStatus } from "../types/vocabulary.ts";

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

    // Object destructuring with property renaming
    // Loading and error states for modifying a single word's status.
    const {
        pending: modifyStatusPending,
        errorMessage: modifyStatusError,
        runAction: runModifyStatus,
        clearError: clearModifyStatusError,
    } = useAsyncAction();

    // Loading and error states for saving statuses of all untracked words.
    const {
        pending: saveStatusesPending,
        errorMessage: saveStatusesError,
        runAction: runSaveStatuses,
    } = useAsyncAction();

    // Store a reference to the textarea DOM element for direct DOM manipulation
    const textareaRef = useRef<HTMLTextAreaElement | null>(null);

    const isEditing = processedText === null;

    // Resize the textarea to fit its content whenever the text or editing mode changes.
    // (useLayoutEffect - runs after the DOM is updated but before the browser paints)
    useLayoutEffect(() => {
        const textarea = textareaRef.current;
        if (!textarea) return;

        textarea.style.height = "auto";
        textarea.style.height = `${textarea.scrollHeight}px`;
    }, [inputText, isEditing]);

    // Process the input text
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

    // Save a status for every untracked word in the text
    async function handleAddAllUntracked(status: VocabularyStatus): Promise<boolean> {
        if (!processedText) return false;

        const untrackedLemmas = getUntrackedLemmas(processedText.tokens);

        return await runSaveStatuses(async () => {
            const addedLemmas = await assignStatusToLemmas(untrackedLemmas, sourceLanguage, targetLanguage, status);
            onWordsStatusChange(sourceLanguage, targetLanguage, addedLemmas, status);
        }, REQUEST_ERRORS.saveStatuses);
    }

    function handleClearConfirm() {
        setClearConfirmOpen(false);
        clearText();
    }

    function handleEdit() {
        setProcessedText(null);
    }

    // Open the word popup and start tracking an untracked word when necessary
    async function handleWordClick(event: MouseEvent<HTMLSpanElement>, token: TextToken) {
        const rect = event.currentTarget.getBoundingClientRect();
        setWordPopupState({
            lemma: token.lemma,
            translation: token.translation,
            top: rect.bottom + 6,
            left: rect.left,
        });
        clearModifyStatusError();

        if (isAuthenticated && token.status === null) {
            await handleStatusChange(token.lemma, "UNKNOWN");
        }
    }

    // Update the status on the backend first, then update it in the text and the dictionaries
    async function handleStatusChange(lemma: string, status: VocabularyStatus) {
        await runModifyStatus(async () => {
            const savedStatus = await saveVocabularyStatus(lemma, sourceLanguage, targetLanguage, status);
            onWordsStatusChange(sourceLanguage, targetLanguage, [lemma], savedStatus);
        }, REQUEST_ERRORS.saveStatus);
    }

    async function handleStatusDelete(lemma: string) {
        await runModifyStatus(async () => {
            await deleteVocabularyStatus(lemma, sourceLanguage, targetLanguage);
            onWordsStatusChange(sourceLanguage, targetLanguage, [lemma], null);
        }, REQUEST_ERRORS.deleteStatus);
    }

    return (
        <div className="mx-auto max-w-3xl p-4">
            <form onSubmit={handleSubmit} className="flex flex-col items-start gap-3">
                {/* Keep the language selector centered and position the clear button on the right. */}
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
                        saveStatusesPending={saveStatusesPending}
                        saveStatusesError={saveStatusesError}
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
                    pending={modifyStatusPending}
                    errorMessage={modifyStatusError}
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

