import { useEffect, useRef } from "react";
import { STATUS_LABELS, STATUS_STYLES, VOCABULARY_STATUSES } from "../lib/vocabulary.ts";
import type { VocabularyStatus } from "../types/text.ts";
import TrashIcon from "./icons/TrashIcon.tsx";

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

const STATUS_BUTTON_CLASSNAME =
    "whitespace-nowrap rounded-md px-1 text-sm transition focus-visible:outline-2 focus-visible:outline-accent disabled:cursor-not-allowed";

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

    // Close on click outside the popup or on Escape
    useEffect(() => {
        function handlePointerDown(event: PointerEvent) {
            if (popupRef.current && !popupRef.current.contains(event.target as Node)) {
                onClose();
            }
        }

        function handleKeyDown(event: KeyboardEvent) {
            if (event.key === "Escape") {
                onClose();
            }
        }

        document.addEventListener("pointerdown", handlePointerDown);
        document.addEventListener("keydown", handleKeyDown);

        return () => {
            document.removeEventListener("pointerdown", handlePointerDown);
            document.removeEventListener("keydown", handleKeyDown);
        };
    }, [onClose]);

    return (
        <div
            ref={popupRef}
            className="fixed z-20 rounded-3xl border border-outline bg-surface-2 text-center text-heading"
            style={{ top, left }}
        >
            {isAuthenticated && status !== null && (
                <button
                    type="button"
                    disabled={pending}
                    onClick={onDelete}
                    aria-label="Remove from vocabulary"
                    title="Remove from vocabulary"
                    className="absolute right-[18px] top-[18px] rounded-md p-1 text-body/60 transition-colors hover:text-heading focus-visible:text-heading disabled:cursor-not-allowed"
                >
                    <TrashIcon />
                </button>
            )}

            <div className="px-5 py-4">
                <div className="px-6 font-medium">{lemma}</div>
                <div className="text-body">–</div>
                <div className="text-body">{translation}</div>

                <div className="mt-3 flex flex-wrap items-center justify-center gap-x-3 gap-y-1 border-t border-outline pt-3">
                    {VOCABULARY_STATUSES.map((buttonStatus) => (
                        <button
                            key={buttonStatus}
                            type="button"
                            disabled={!isAuthenticated || pending}
                            onClick={() => onStatusChange(buttonStatus)}
                            className={`${STATUS_BUTTON_CLASSNAME} ${isAuthenticated ? "" : "opacity-40"} ${
                                buttonStatus === status
                                    ? STATUS_STYLES[buttonStatus].segmentSelected
                                    : STATUS_STYLES[buttonStatus].segment
                            }`}
                        >
                            {"> "}{STATUS_LABELS[buttonStatus]}
                        </button>
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