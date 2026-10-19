import { useTranslation } from "react-i18next";
import ModalDialog from "./ModalDialog.tsx";

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

    return (
        <ModalDialog title={title} closeDisabled={pending} onClose={onCancel}>
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
        </ModalDialog>
    );
}
