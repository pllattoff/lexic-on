import { useRef } from "react";
import TrashIcon from "../icons/TrashIcon.tsx";
import CircleButton from "../ui/CircleButton.tsx";
import StatusButton from "../ui/StatusButton.tsx";
import { useDismiss } from "../../hooks/useDismiss.ts";
import { VOCABULARY_STATUSES } from "../../lib/vocabulary.ts";
import type { VocabularyStatus } from "../../types/vocabulary.ts";

type WordPopupProps = {
    lemma: string;
    translation: string;
    status: VocabularyStatus | null;
    isAuthenticated: boolean;
    pending: boolean;
    errorMessage: string | null;
    top: number;
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
                                      top,
                                      left,
                                      onStatusChange,
                                      onDelete,
                                      onClose,
                                  }: Readonly<WordPopupProps>) {
    const popupRef = useRef<HTMLDivElement | null>(null);

    // The popup exists only while it is open
    useDismiss(popupRef, true, onClose);

    return (
        <div
            ref={popupRef}
            className="fixed z-20 rounded-3xl border border-outline bg-surface-2 text-center text-heading"
            style={{ top, left }}
        >
            {isAuthenticated && status !== null && (
                <CircleButton
                    label="Remove from vocabulary"
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
                <div className="text-body">–</div>
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
                    <div className="mt-2 text-sm text-body/60">Log in to track words</div>
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