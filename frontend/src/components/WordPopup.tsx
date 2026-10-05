import { useEffect, useRef } from "react";

type WordPopupProps = {
    lemma: string;
    translation: string;
    top: number;
    left: number;
    onClose: () => void;
};

export default function WordPopup({ lemma, translation, top, left, onClose }: Readonly<WordPopupProps>) {
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
            className="fixed z-20 rounded-3xl border border-outline bg-surface-2 px-9 py-4 text-center text-heading"
            style={{ top, left }}
        >
            <div className="font-medium">{lemma}</div>
            <div className="text-body">–</div>
            <div className="text-body">{translation}</div>
        </div>
    );
}