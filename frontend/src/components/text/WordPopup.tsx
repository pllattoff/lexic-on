import { useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import TrashIcon from "../icons/TrashIcon.tsx";
import CircleButton from "../ui/CircleButton.tsx";
import StatusButton from "../ui/StatusButton.tsx";
import { useDismiss } from "../../hooks/useDismiss.ts";
import { usePopupPosition } from "../../hooks/usePopupPosition.ts";
import { VOCABULARY_STATUSES } from "../../lib/vocabulary.ts";
import type { VocabularyStatus } from "../../types/vocabulary.ts";

type WordPopupProps = {
    lemma: string;
    translation: string;
    status: VocabularyStatus | null;
    isAuthenticated: boolean;
    pending: boolean;
    errorMessage: string | null;
    // Position of the clicked word on the screen
    wordTop: number;
    wordBottom: number;
    left: number;
    onStatusChange: (status: VocabularyStatus) => void;
    onDelete: () => void;
    onClose: () => void;
};

export default function WordPopup({
                                      lemma,
                                      translation,
                                      status,
                                      isAuthenticated,
                                      pending,
                                      errorMessage,
                                      wordTop,
                                      wordBottom,
                                      left,
                                      onStatusChange,
                                      onDelete,
                                      onClose,
                                  }: Readonly<WordPopupProps>) {
    const { t } = useTranslation();
    const popupRef = useRef<HTMLDivElement | null>(null);

    // The popup exists only while it is open
    useDismiss(popupRef, true, onClose);

    // The popup is fixed on the screen, so it would stay in place while the word moves away on scrolling.
    // Scrolling inside the popup does not count: scroll events of elements do not reach the window.
    useEffect(() => {
        window.addEventListener("scroll", onClose);
        return () => window.removeEventListener("scroll", onClose);
    }, [onClose]);

    usePopupPosition(popupRef, { top: wordTop, bottom: wordBottom, left });

    return (
        <div
            ref={popupRef}
            // w-max keeps the natural width regardless of the position, so the text wraps
            // only when the popup is wider than the whole screen
            className="fixed z-20 w-max max-w-[calc(100vw-1rem)] overflow-y-auto rounded-3xl border border-outline bg-surface-2 text-center text-heading shadow-lg shadow-black/40"
        >
            {isAuthenticated && status !== null && (
                <CircleButton
                    label={t("common.removeFromVocabulary")}
                    variant="inPopup"
                    disabled={pending}
                    className="absolute right-[17px] top-[17px]"
                    onClick={onDelete}
                >
                    <TrashIcon />
                </CircleButton>
            )}

            <div className="px-5 py-4">
                <div className="px-6 font-medium">{lemma}</div>
                <div className="text-body/40">–</div>
                <div className="text-body">{translation}</div>

                <div className="mt-3 flex flex-wrap items-center justify-center gap-x-3 gap-y-1 border-t border-outline pt-3">
                    {VOCABULARY_STATUSES.map((buttonStatus) => (
                        <StatusButton
                            key={buttonStatus}
                            status={buttonStatus}
                            selected={buttonStatus === status}
                            disabled={!isAuthenticated || pending}
                            dimmed={!isAuthenticated}
                            onClick={() => onStatusChange(buttonStatus)}
                        />
                    ))}
                </div>

                {!isAuthenticated && (
                    <div className="mt-2 text-sm text-body/60">{t("common.loginToTrack")}</div>
                )}

                {errorMessage && (
                    <div role="alert" className="mt-2 text-sm text-error">
                        {errorMessage}
                    </div>
                )}
            </div>
        </div>
    );
}