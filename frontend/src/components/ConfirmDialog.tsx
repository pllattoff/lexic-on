import { useEffect, useId } from "react";

type ConfirmDialogProps = {
    title: string;
    message: string;
    confirmLabel: string;
    // A red confirm button for actions that destroy data
    danger?: boolean;
    pending?: boolean;
    errorMessage?: string | null;
    onConfirm: () => void;
    onCancel: () => void;
};

const CONFIRM_BUTTON_CLASSNAME = "rounded-md border px-4 py-2 font-medium text-heading transition disabled:cursor-not-allowed disabled:opacity-40";
const CONFIRM_DANGER_CLASSNAME = "border-unknown/40 bg-unknown/20 hover:bg-unknown/30";
const CONFIRM_DEFAULT_CLASSNAME = "border-accent/40 bg-accent/20 hover:bg-accent/30";

// A modal window that asks the user to confirm an action
export default function ConfirmDialog({
                                          title,
                                          message,
                                          confirmLabel,
                                          danger = false,
                                          pending = false,
                                          errorMessage = null,
                                          onConfirm,
                                          onCancel,
                                      }: Readonly<ConfirmDialogProps>) {
    const titleId = useId();

    // Cancel on Escape, except while the action runs
    useEffect(() => {
        function handleKeyDown(event: KeyboardEvent) {
            if (event.key === "Escape" && !pending) {
                onCancel();
            }
        }

        document.addEventListener("keydown", handleKeyDown);

        return () => document.removeEventListener("keydown", handleKeyDown);
    }, [onCancel, pending]);

    return (
        // Clicking the dark background cancels
        <div
            className="fixed inset-0 z-30 flex items-center justify-center bg-black/60 p-4 backdrop-blur-[2px]"
            onPointerDown={(event) => {
                if (event.target === event.currentTarget && !pending) onCancel();
            }}
        >
            <div
                role="dialog"
                aria-modal="true"
                aria-labelledby={titleId}
                className="w-full max-w-sm rounded-2xl border border-outline bg-surface-2 px-6 pb-6 pt-7 text-center shadow-2xl shadow-black/50"
            >
                <h2 id={titleId} className="text-xl font-semibold text-heading">{title}</h2>

                <p className="mt-3 text-sm leading-relaxed text-body/80">{message}</p>

                {errorMessage && (
                    <div role="alert" className="mt-4 rounded-md bg-error/10 px-3 py-2 text-sm text-error">
                        {errorMessage}
                    </div>
                )}

                <div className="mt-7 grid grid-cols-2 gap-3">
                    <button
                        type="button"
                        autoFocus
                        disabled={pending}
                        onClick={onCancel}
                        className="rounded-md border border-outline px-4 py-2 text-body transition hover:border-outline-strong hover:bg-outline/30 hover:text-heading disabled:cursor-not-allowed disabled:opacity-40"
                    >
                        Cancel
                    </button>
                    <button
                        type="button"
                        disabled={pending}
                        onClick={onConfirm}
                        className={`${CONFIRM_BUTTON_CLASSNAME} ${danger ? CONFIRM_DANGER_CLASSNAME : CONFIRM_DEFAULT_CLASSNAME}`}
                    >
                        {confirmLabel}
                    </button>
                </div>
            </div>
        </div>
    );
}
