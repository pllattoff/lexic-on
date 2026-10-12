import { useEffect, type RefObject } from "react";

// Calls onClose on a click outside the element or on Escape, while the element is open.
// Used by the popups.
export function useDismiss(ref: RefObject<HTMLElement | null>, open: boolean, onClose: () => void) {
    useEffect(() => {
        if (!open) return;

        function handlePointerDown(event: PointerEvent) {
            if (ref.current && !ref.current.contains(event.target as Node)) {
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
    }, [ref, open, onClose]);
}
