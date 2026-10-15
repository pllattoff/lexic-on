import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router";
import { deleteDictionary } from "../api/dictionaryApi.ts";
import TrashIcon from "../components/icons/TrashIcon.tsx";
import CircleButton from "../components/ui/CircleButton.tsx";
import ConfirmDialog from "../components/ui/ConfirmDialog.tsx";
import { useAsyncAction } from "../hooks/useAsyncAction.ts";
import { dictionaryPath, dictionaryStatusPath } from "../lib/dictionaryRoutes.ts";
import { LANGUAGE_LABELS, type SourceLanguage, type TargetLanguage } from "../lib/languages.ts";
import { PAGE_CONTAINER_CLASSNAME, PAGE_TITLE_CLASSNAME } from "../lib/pageStyles.ts";
import { REQUEST_ERRORS } from "../lib/requestErrors.ts";
import { STATUS_LABEL_KEYS, VOCABULARY_STATUSES } from "../lib/vocabulary.ts";
import { STATUS_STYLES, STATUS_TILE_CLASSNAME } from "../lib/vocabularyStyles.ts";
import type { DictionarySummary } from "../types/dictionary.ts";

type DictionariesPageProps = {
    // null until the first response arrives
    dictionaries: DictionarySummary[] | null;
    onDictionaryDeleted: (sourceLanguage: SourceLanguage, targetLanguage: TargetLanguage) => void;
};

function countWords({ wordCounts }: DictionarySummary): number {
    return Object.values(wordCounts).reduce((sum, count) => sum + count, 0);
}

export default function DictionariesPage({ dictionaries, onDictionaryDeleted }: Readonly<DictionariesPageProps>) {
    const { t } = useTranslation();
    const [dictionaryToDelete, setDictionaryToDelete] = useState<DictionarySummary | null>(null);
    const { pending: deletePending, errorMessage: deleteError, runAction, clearError: clearDeleteError } = useAsyncAction();

    function openDeleteDialog(dictionary: DictionarySummary) {
        clearDeleteError();
        setDictionaryToDelete(dictionary);
    }

    async function handleDeleteConfirm(dictionary: DictionarySummary) {
        await runAction(async () => {
            await deleteDictionary(dictionary.sourceLanguage, dictionary.targetLanguage);
            setDictionaryToDelete(null);
            onDictionaryDeleted(dictionary.sourceLanguage, dictionary.targetLanguage);
        }, t(REQUEST_ERRORS.deleteDictionary));
    }

    return (
        <div className={PAGE_CONTAINER_CLASSNAME}>
            <h1 className={PAGE_TITLE_CLASSNAME}>{t("common.dictionaries")}</h1>

            {dictionaries?.length === 0 && (
                <p>{t("dictionaries.empty")}</p>
            )}

            <div className="flex flex-col gap-4">
                {dictionaries?.map((dictionary) => {
                    const { sourceLanguage, targetLanguage, wordCounts } = dictionary;

                    return (
                        <section key={dictionaryPath(sourceLanguage, targetLanguage)}
                                 className="rounded-lg bg-panel p-4">
                            <div className="mb-3 flex items-center justify-between gap-2">
                                <h2 className="text-lg font-medium">
                                    <Link to={dictionaryPath(sourceLanguage, targetLanguage)}
                                          className="text-body/70 transition-colors hover:text-heading">
                                        {LANGUAGE_LABELS[sourceLanguage]} → {LANGUAGE_LABELS[targetLanguage]}
                                    </Link>
                                </h2>

                                <CircleButton
                                    label={t("dictionaries.delete")}
                                    className="mr-1"
                                    onClick={() => openDeleteDialog(dictionary)}
                                >
                                    <TrashIcon />
                                </CircleButton>
                            </div>

                            <div className="grid grid-cols-1 gap-2 min-[400px]:grid-cols-3">
                                {VOCABULARY_STATUSES.map((status) => (
                                    <Link
                                        key={status}
                                        to={dictionaryStatusPath(sourceLanguage, targetLanguage, status)}
                                        className={`${STATUS_TILE_CLASSNAME} transition ${STATUS_STYLES[status].block} ${STATUS_STYLES[status].blockHover}`}
                                    >
                                        <div className="font-medium">{t(STATUS_LABEL_KEYS[status])}</div>
                                        <div className="text-sm">{wordCounts[status]}</div>
                                    </Link>
                                ))}
                            </div>
                        </section>
                    );
                })}
            </div>

            {dictionaryToDelete && (
                <ConfirmDialog
                    title={t("dictionaries.deleteTitle")}
                    message={t("dictionaries.deleteMessage", {
                        count: countWords(dictionaryToDelete),
                        source: LANGUAGE_LABELS[dictionaryToDelete.sourceLanguage],
                        target: LANGUAGE_LABELS[dictionaryToDelete.targetLanguage],
                    })}
                    confirmLabel={t("common.delete")}
                    danger
                    pending={deletePending}
                    errorMessage={deleteError}
                    onConfirm={() => void handleDeleteConfirm(dictionaryToDelete)}
                    onCancel={() => setDictionaryToDelete(null)}
                />
            )}
        </div>
    );
}
