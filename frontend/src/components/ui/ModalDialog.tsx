import { useEffect, useId, useRef, type ReactNode, type SyntheticEvent } from "react";

type ModalDialogProps = {
    title: string;
    // True while an action runs: Escape is ignored, so the window can't be closed in the middle of it
    closeDisabled?: boolean;
    onClose: () => void;
    children: ReactNode;
};

// A modal window with a title; its content is passed as children
export default function ModalDialog({
                                        title,
                                        closeDisabled = false,
                                        onClose,
                                        children,
                                    }: Readonly<ModalDialogProps>) {
    const titleId = useId();
    const dialogRef = useRef<HTMLDialogElement | null>(null);

    // showModal() makes the rest of the page inert and gives the dark backdrop
    useEffect(() => {
        const dialog = dialogRef.current;
        if (dialog && !dialog.open) dialog.showModal();
    }, []);

    // The browser fires "cancel" on Escape. It is always prevented, so the dialog closes only when the parent
    // stops rendering it, and Escape is ignored while closing is disabled.
    function handleCancelEvent(event: SyntheticEvent<HTMLDialogElement>) {
        event.preventDefault();
        if (!closeDisabled) onClose();
    }

    return (
        <dialog
            ref={dialogRef}
            aria-labelledby={titleId}
            onCancel={handleCancelEvent}
            className="m-auto w-full max-w-[min(24rem,calc(100vw-2rem))] rounded-2xl border border-outline bg-surface-2 px-5 pb-5 pt-6 text-center text-body shadow-2xl shadow-black/50 backdrop:bg-black/60 backdrop:backdrop-blur-[2px]"
        >
            <h2 id={titleId} className="text-xl font-semibold text-heading">{title}</h2>

            {children}
        </dialog>
    );
}
