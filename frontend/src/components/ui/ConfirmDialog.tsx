import { useEffect, useId, useRef, type SyntheticEvent } from "react";
import { useTranslation } from "react-i18next";

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

const CANCEL_BUTTON_CLASSNAME =
    "rounded-md border border-outline px-4 py-2 text-body transition hover:border-outline-strong hover:bg-outline/30 hover:text-heading disabled:cursor-not-allowed disabled:opacity-40";
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
    const { t } = useTranslation();
    const titleId = useId();
    const dialogRef = useRef<HTMLDialogElement | null>(null);

    // showModal() makes the rest of the page inert and gives the dark backdrop
    useEffect(() => {
        const dialog = dialogRef.current;
        if (dialog && !dialog.open) dialog.showModal();
    }, []);

    // The browser fires "cancel" on Escape. It is always prevented, so the dialog closes only when the parent
    // stops rendering it, and Escape is ignored while the action runs.
    function handleCancelEvent(event: SyntheticEvent<HTMLDialogElement>) {
        event.preventDefault();
        if (!pending) onCancel();
    }

    return (
        <dialog
            ref={dialogRef}
            aria-labelledby={titleId}
            onCancel={handleCancelEvent}
            className="m-auto w-full max-w-[min(24rem,calc(100vw-2rem))] rounded-2xl border border-outline bg-surface-2 px-6 pb-6 pt-7 text-center text-body shadow-2xl shadow-black/50 backdrop:bg-black/60 backdrop:backdrop-blur-[2px]"
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
                    className={CANCEL_BUTTON_CLASSNAME}
                >
                    {t("common.cancel")}
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
        </dialog>
    );
}
