import { useTranslation } from "react-i18next";
import UntrackedTile from "./UntrackedTile.tsx";
import { getUntrackedLemmas, STATUS_LABEL_KEYS, VOCABULARY_STATUSES, type HighlightKey } from "../../lib/vocabulary.ts";
import { STATUS_STYLES, STATUS_TILE_CLASSNAME } from "../../lib/vocabularyStyles.ts";
import type { TextToken } from "../../types/text.ts";
import type { VocabularyStatus } from "../../types/vocabulary.ts";

type VocabularyStatsBarProps = {
    tokens: TextToken[];
    hiddenHighlights: ReadonlySet<HighlightKey>;
    isAuthenticated: boolean;
    saveStatusesPending: boolean;
    saveStatusesError: string | null;
    onHighlightToggle: (key: HighlightKey) => void;
    // Resolves to whether the words were added
    onAddAllUntracked: (status: VocabularyStatus) => Promise<boolean>;
};

// Each word occurrence counts, so the percentages of all blocks add up to 100%
export default function VocabularyStatsBar({
                                               tokens,
                                               hiddenHighlights,
                                               isAuthenticated,
                                               saveStatusesPending,
                                               saveStatusesError,
                                               onHighlightToggle,
                                               onAddAllUntracked,
                                           }: Readonly<VocabularyStatsBarProps>) {
    const { t } = useTranslation();

    if (tokens.length === 0) return null;

    function percentage(count: number) {
        return Math.round((count / tokens.length) * 100);
    }

    const untrackedCount = tokens.filter((token) => token.status === null).length;
    // The tile counts every occurrence, the "Add" popup counts unique lemmas
    const untrackedLemmaCount = getUntrackedLemmas(tokens).length;

    return (
        <div className="grid w-full grid-cols-2 gap-2 sm:grid-cols-4">
            <UntrackedTile
                count={untrackedCount}
                lemmaCount={untrackedLemmaCount}
                percentage={percentage(untrackedCount)}
                highlighted={!hiddenHighlights.has("UNTRACKED")}
                isAuthenticated={isAuthenticated}
                pending={saveStatusesPending}
                errorMessage={saveStatusesError}
                onToggle={() => onHighlightToggle("UNTRACKED")}
                onAddAll={onAddAllUntracked}
            />

            {/* Clicking a status tile turns the highlighting of its words in the text on or off */}
            {VOCABULARY_STATUSES.map((status) => {
                const highlighted = !hiddenHighlights.has(status);
                const count = tokens.filter((token) => token.status === status).length;
                const styles = STATUS_STYLES[status];
                const stateClassName = highlighted ? `${styles.block} ${styles.blockHover}` : styles.tab;
                const statusLabel = t(STATUS_LABEL_KEYS[status]);
                const hint = highlighted
                    ? t("stats.hideHighlighting", { status: statusLabel })
                    : t("stats.showHighlighting", { status: statusLabel });

                return (
                    <button
                        key={status}
                        type="button"
                        title={hint}
                        aria-pressed={highlighted}
                        onClick={() => onHighlightToggle(status)}
                        className={`${STATUS_TILE_CLASSNAME} cursor-pointer transition ${stateClassName}`}
                    >
                        <div className="font-medium">{statusLabel}</div>
                        <div className="text-sm">{percentage(count)}% ({count})</div>
                    </button>
                );
            })}
        </div>
    );
}
