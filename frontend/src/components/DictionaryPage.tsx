import { useEffect, useState } from "react";
import { Link, NavLink, useParams } from "react-router";
import ArrowLeftIcon from "./icons/ArrowLeftIcon.tsx";
import TrashIcon from "./icons/TrashIcon.tsx";
import NotFoundPage from "./NotFoundPage.tsx";
import StatusButton from "./StatusButton.tsx";
import { getDictionaryWords } from "../api/dictionaryApi.ts";
import { deleteVocabularyStatus, saveVocabularyStatus } from "../api/vocabularyApi.ts";
import { useStatusRequest } from "../hooks/useStatusRequest.ts";
import { dictionaryStatusPath, parseLanguagePair, parseStatus } from "../lib/dictionaryRoutes.ts";
import { LANGUAGE_LABELS, type SourceLanguage, type TargetLanguage } from "../lib/languages.ts";
import { STATUS_LABELS, STATUS_STYLES, STATUS_TILE_CLASSNAME, VOCABULARY_STATUSES } from "../lib/vocabulary.ts";
import type { DictionaryWord, WordsStatusChangeHandler } from "../types/dictionary.ts";
import type { VocabularyStatus } from "../types/text.ts";

export default function DictionaryPage({ onWordsStatusChange }: Readonly<{ onWordsStatusChange: WordsStatusChangeHandler }>) {
    const { pair, status: statusSlug } = useParams();
    const languagePair = parseLanguagePair(pair);
    const status = parseStatus(statusSlug);

    if (!languagePair || !status) return <NotFoundPage />;

    // The key makes a new tab or a new dictionary start with a fresh state instead of the previous word list
    return (
        <DictionaryContent
            key={`${pair}/${statusSlug}`}
            sourceLanguage={languagePair.sourceLanguage}
            targetLanguage={languagePair.targetLanguage}
            status={status}
            onWordsStatusChange={onWordsStatusChange}
        />
    );
}

type DictionaryContentProps = {
    sourceLanguage: SourceLanguage;
    targetLanguage: TargetLanguage;
    status: VocabularyStatus;
    onWordsStatusChange: WordsStatusChangeHandler;
};

function DictionaryContent({ sourceLanguage, targetLanguage, status, onWordsStatusChange }: Readonly<DictionaryContentProps>) {
    // null until the words are loaded
    const [words, setWords] = useState<DictionaryWord[] | null>(null);
    const [loadError, setLoadError] = useState(false);
    const { pending, errorMessage, runStatusRequest } = useStatusRequest();

    useEffect(() => {
        // Ignore the response if the page was left or the tab was changed meanwhile
        let ignore = false;

        async function loadWords() {
            try {
                const loadedWords = await getDictionaryWords(sourceLanguage, targetLanguage, status);
                if (!ignore) setWords(loadedWords);
            } catch (error) {
                console.error("Failed to load dictionary words:", error);
                if (!ignore) setLoadError(true);
            }
        }

        void loadWords();

        return () => {
            ignore = true;
        };
    }, [sourceLanguage, targetLanguage, status]);

    // The word leaves this tab: it moves to another status or is deleted
    function removeWord(lemma: string) {
        setWords((current) => current && current.filter((word) => word.lemma !== lemma));
    }

    async function handleStatusChange(lemma: string, newStatus: VocabularyStatus) {
        await runStatusRequest(async () => {
            await saveVocabularyStatus(lemma, sourceLanguage, targetLanguage, newStatus);
            removeWord(lemma);
            onWordsStatusChange(sourceLanguage, targetLanguage, [lemma], newStatus);
        });
    }

    async function handleDelete(lemma: string) {
        await runStatusRequest(async () => {
            await deleteVocabularyStatus(lemma, sourceLanguage, targetLanguage);
            removeWord(lemma);
            onWordsStatusChange(sourceLanguage, targetLanguage, [lemma], null);
        });
    }

    return (
        <div className="mx-auto max-w-3xl p-4">
            <div className="mb-4 flex items-center gap-3">
                <Link to="/dictionaries" aria-label="Back to dictionaries" title="Back to dictionaries"
                      className="rounded-md p-1 text-body hover:text-heading">
                    <ArrowLeftIcon />
                </Link>
                <h1 className="text-lg font-medium text-body/70">
                    {LANGUAGE_LABELS[sourceLanguage]} → {LANGUAGE_LABELS[targetLanguage]}
                </h1>
            </div>

            <nav className="mb-4 grid grid-cols-3 gap-2">
                {VOCABULARY_STATUSES.map((tabStatus) => (
                    <NavLink
                        key={tabStatus}
                        to={dictionaryStatusPath(sourceLanguage, targetLanguage, tabStatus)}
                        className={({ isActive }) =>
                            `${STATUS_TILE_CLASSNAME} ${isActive ? STATUS_STYLES[tabStatus].tabSelected : STATUS_STYLES[tabStatus].tab}`
                        }
                    >
                        {STATUS_LABELS[tabStatus]}
                    </NavLink>
                ))}
            </nav>

            {loadError && <p role="alert" className="text-error">Couldn't load the words. Try again.</p>}

            {words?.length === 0 && <p>No words with this status yet.</p>}

            <ul>
                {words?.map((word) => (
                    <li key={word.lemma}
                        className="flex flex-col gap-1 border-b border-outline py-2 sm:flex-row sm:items-center sm:justify-between">
                        <div className="min-w-0">
                            <span className="font-medium text-heading">{word.lemma}</span> – {word.translation}
                        </div>

                        <div className="flex shrink-0 items-center gap-3">
                            {VOCABULARY_STATUSES.filter((otherStatus) => otherStatus !== status).map((otherStatus) => (
                                <StatusButton
                                    key={otherStatus}
                                    status={otherStatus}
                                    disabled={pending}
                                    onClick={() => handleStatusChange(word.lemma, otherStatus)}
                                />
                            ))}

                            <button
                                type="button"
                                disabled={pending}
                                onClick={() => handleDelete(word.lemma)}
                                aria-label="Remove from vocabulary"
                                title="Remove from vocabulary"
                                className="rounded-md p-1 text-body/60 transition-colors hover:text-heading focus-visible:text-heading disabled:cursor-not-allowed"
                            >
                                <TrashIcon />
                            </button>
                        </div>
                    </li>
                ))}
            </ul>

            {errorMessage && (
                <p role="alert" className="mt-3 text-sm text-error">{errorMessage}</p>
            )}
        </div>
    );
}