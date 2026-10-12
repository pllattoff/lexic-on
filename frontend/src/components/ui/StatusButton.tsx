import { STATUS_LABELS } from "../../lib/vocabulary.ts";
import { STATUS_STYLES } from "../../lib/vocabularyStyles.ts";
import type { VocabularyStatus } from "../../types/vocabulary.ts";

const BUTTON_CLASSNAME =
    "whitespace-nowrap rounded-md px-1 text-sm transition focus-visible:outline-2 focus-visible:outline-accent disabled:cursor-not-allowed";

type StatusButtonProps = {
    status: VocabularyStatus;
    // The current status of the word is highlighted
    selected?: boolean;
    disabled?: boolean;
    // Dimmed for guests, who cannot change statuses
    dimmed?: boolean;
    onClick: () => void;
};

// A text button for selecting a vocabulary status.
// Used in the word popup, dictionary rows, and the "add all" popup.
export default function StatusButton({
                                         status,
                                         selected = false,
                                         disabled = false,
                                         dimmed = false,
                                         onClick,
                                     }: Readonly<StatusButtonProps>) {
    const statusClassName = selected ? STATUS_STYLES[status].segmentSelected : STATUS_STYLES[status].segment;

    return (
        <button
            type="button"
            disabled={disabled}
            onClick={onClick}
            className={`${BUTTON_CLASSNAME} ${dimmed ? "opacity-40" : ""} ${statusClassName}`}
        >
            {"> "}{STATUS_LABELS[status]}
        </button>
    );
}
