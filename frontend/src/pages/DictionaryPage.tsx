import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, NavLink, useParams } from "react-router";
import { getDictionaryWords } from "../api/dictionaryApi.ts";
import { deleteVocabularyStatus, saveVocabularyStatus } from "../api/vocabularyApi.ts";
import ArrowLeftIcon from "../components/icons/ArrowLeftIcon.tsx";
import TrashIcon from "../components/icons/TrashIcon.tsx";
import StatusButton from "../components/ui/StatusButton.tsx";
import { useAsyncAction } from "../hooks/useAsyncAction.ts";
import { dictionaryStatusPath, parseLanguagePair, parseStatus } from "../lib/dictionaryRoutes.ts";
import { LANGUAGE_LABELS, type SourceLanguage, type TargetLanguage } from "../lib/languages.ts";
import { PAGE_CONTAINER_CLASSNAME } from "../lib/pageStyles.ts";
import { REQUEST_ERRORS } from "../lib/requestErrors.ts";
import { STATUS_LABEL_KEYS, VOCABULARY_STATUSES } from "../lib/vocabulary.ts";
import { STATUS_STYLES, STATUS_TILE_CLASSNAME } from "../lib/vocabularyStyles.ts";
import NotFoundPage from "./NotFoundPage.tsx";
import type { DictionaryWord, WordsStatusChangeHandler } from "../types/dictionary.ts";
import type { VocabularyStatus } from "../types/vocabulary.ts";

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
    const { t } = useTranslation();
    const [words, setWords] = useState<DictionaryWord[] | null>(null);
    const [loadError, setLoadError] = useState(false);
    const { pending, errorMessage, runAction } = useAsyncAction();

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

    // Remove the word from the local list when its status changes or it is deleted
    function removeWord(lemma: string) {
        setWords((current) => {
            if (!current) return current;

            return current.filter((word) => word.lemma !== lemma);
        });
    }

    async function handleStatusChange(lemma: string, newStatus: VocabularyStatus) {
        await runAction(async () => {
            await saveVocabularyStatus(lemma, sourceLanguage, targetLanguage, newStatus);
            removeWord(lemma);
            onWordsStatusChange(sourceLanguage, targetLanguage, [lemma], newStatus);
        }, t(REQUEST_ERRORS.saveStatus));
    }

    async function handleDelete(lemma: string) {
        await runAction(async () => {
            await deleteVocabularyStatus(lemma, sourceLanguage, targetLanguage);
            removeWord(lemma);
            onWordsStatusChange(sourceLanguage, targetLanguage, [lemma], null);
        }, t(REQUEST_ERRORS.deleteStatus));
    }

    return (
        <div className={PAGE_CONTAINER_CLASSNAME}>
            <div className="mb-4 flex items-center gap-3">
                <Link to="/dictionaries" aria-label={t("dictionary.backToDictionaries")} title={t("dictionary.backToDictionaries")}
                      className="rounded-md p-1 text-body hover:text-heading">
                    <ArrowLeftIcon />
                </Link>
                <h1 className="text-lg font-medium text-body/70">
                    {LANGUAGE_LABELS[sourceLanguage]} → {LANGUAGE_LABELS[targetLanguage]}
                </h1>
            </div>

            {/* minmax(auto, 1fr): equal columns, but a column never gets narrower than its longest label */}
            <nav className="mb-4 grid grid-cols-[repeat(3,minmax(auto,1fr))] gap-2 max-[399px]:gap-1">
                {VOCABULARY_STATUSES.map((tabStatus) => (
                    <NavLink
                        key={tabStatus}
                        to={dictionaryStatusPath(sourceLanguage, targetLanguage, tabStatus)}
                        className={({ isActive }) =>
                            `${STATUS_TILE_CLASSNAME} max-[399px]:px-1 max-[339px]:text-sm ${isActive ? STATUS_STYLES[tabStatus].tabSelected : STATUS_STYLES[tabStatus].tab}`
                        }
                    >
                        {t(STATUS_LABEL_KEYS[tabStatus])}
                    </NavLink>
                ))}
            </nav>

            {loadError && <p role="alert" className="text-error">{t("dictionary.loadError")}</p>}

            {words?.length === 0 && <p>{t("dictionary.empty")}</p>}

            <ul>
                {words?.map((word) => (
                    <li key={word.lemma}
                        className="flex flex-col gap-1 border-b border-outline py-2 sm:flex-row sm:items-center sm:justify-between">
                        <div className="min-w-0">
                            <span className="font-medium text-heading">{word.lemma}</span> – {word.translation}
                        </div>

                        <div className="flex shrink-0 items-center justify-end gap-3">
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
                                aria-label={t("common.removeFromVocabulary")}
                                title={t("common.removeFromVocabulary")}
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