import { useCallback, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import ChevronDownIcon from "../icons/ChevronDownIcon.tsx";
import StatusButton from "../ui/StatusButton.tsx";
import { useDismiss } from "../../hooks/useDismiss.ts";
import { VOCABULARY_STATUSES } from "../../lib/vocabulary.ts";
import { UNTRACKED_STYLES } from "../../lib/vocabularyStyles.ts";
import type { VocabularyStatus } from "../../types/vocabulary.ts";

type UntrackedTileProps = {
    count: number;
    // Number of unique lemmas added by the "Add all" action
    lemmaCount: number;
    percentage: number;
    highlighted: boolean;
    isAuthenticated: boolean;
    pending: boolean;
    errorMessage: string | null;
    onToggle: () => void;
    // Resolves to whether the words were added
    onAddAll: (status: VocabularyStatus) => Promise<boolean>;
};

// The tile of the words without a status. Clicking the tile switches their highlighting,
// the chevron opens a popup to add all of them to the vocabulary with one status
export default function UntrackedTile({
                                          count,
                                          lemmaCount,
                                          percentage,
                                          highlighted,
                                          isAuthenticated,
                                          pending,
                                          errorMessage,
                                          onToggle,
                                          onAddAll,
                                      }: Readonly<UntrackedTileProps>) {
    const { t } = useTranslation();
    const [popupOpen, setPopupOpen] = useState(false);
    // Includes the tile and popup, so clicks inside them are not treated as outside clicks
    const containerRef = useRef<HTMLDivElement | null>(null);

    const closePopup = useCallback(() => setPopupOpen(false), []);
    useDismiss(containerRef, popupOpen, closePopup);

    // Keep the popup open while the request is pending or fails
    async function handleAddAll(status: VocabularyStatus) {
        const added = await onAddAll(status);
        if (added) setPopupOpen(false);
    }

    const tileClassName = highlighted ? UNTRACKED_STYLES.tile : UNTRACKED_STYLES.tileDimmed;
    const partHoverClassName = highlighted ? UNTRACKED_STYLES.tileHover : UNTRACKED_STYLES.tileDimmedHover;

    return (
        <div ref={containerRef} className="relative">
            {/* A split button: the chevron part and the main part share one border and are highlighted separately */}
            <div className={`flex overflow-hidden rounded-md border transition ${tileClassName}`}>
                {count > 0 && (
                    <button
                        type="button"
                        aria-label={t("untracked.addAllLabel")}
                        title={t("untracked.addAllTitle")}
                        aria-expanded={popupOpen}
                        onClick={() => setPopupOpen((open) => !open)}
                        className={`relative flex w-8 cursor-pointer items-center justify-center border-inherit text-body/60 transition hover:text-heading ${partHoverClassName}`}
                    >
                        <span className={`block transition-transform ${popupOpen ? "rotate-180" : ""}`}>
                            <ChevronDownIcon />
                        </span>

                        {/* The divider is shorter than the tile and takes the border color of the tile */}
                        <span className="absolute inset-y-2 right-0 border-r border-inherit" />
                    </button>
                )}

                <button
                    type="button"
                    title={highlighted ? t("untracked.hideHighlighting") : t("untracked.showHighlighting")}
                    aria-pressed={highlighted}
                    onClick={onToggle}
                    className={`flex-1 cursor-pointer px-3 py-2 text-center transition ${partHoverClassName}`}
                >
                    <div className="font-medium">{t("untracked.label")}</div>
                    <div className="text-sm">{percentage}% ({count})</div>
                </button>
            </div>

            {popupOpen && (
                // At least as wide as the tile and as wide as its content needs, so the text and the buttons
                // wrap only when the content does not fit on the screen
                <div className="absolute left-0 top-full z-20 mt-2 w-max min-w-full max-w-[calc(100vw-3rem)] rounded-2xl border border-outline bg-surface-2 px-4 py-3 text-center">
                    <div className="text-sm text-body">
                        {t("untracked.addPopup", { count: lemmaCount })}
                    </div>

                    <div className="mt-2 flex flex-wrap items-center justify-center gap-x-3 gap-y-1 border-t border-outline pt-2">
                        {VOCABULARY_STATUSES.map((status) => (
                            <StatusButton
                                key={status}
                                status={status}
                                disabled={!isAuthenticated || pending}
                                dimmed={!isAuthenticated}
                                onClick={() => void handleAddAll(status)}
                            />
                        ))}
                    </div>

                    {!isAuthenticated && <div className="mt-2 text-sm text-body/60">{t("common.loginToTrack")}</div>}

                    {errorMessage && (
                        <div role="alert" className="mt-2 text-sm text-error">
                            {errorMessage}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
