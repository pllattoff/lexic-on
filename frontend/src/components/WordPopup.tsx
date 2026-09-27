type WordPopupProps = {
    lemma: string;
    top: number;
    left: number;
    onClose: () => void;
};

export default function WordPopup({ lemma, top, left, onClose }: Readonly<WordPopupProps>) {
    return (
        <>
            {/* Full-screen invisible overlay on a lower layer (z-10) that triggers popup closing when clicked */}
            <div className="fixed inset-0 z-10" onClick={onClose} />

            {/* Popup on a higher layer (z-20) above the overlay */}
            <div
                className="fixed z-20 rounded-lg border border-outline bg-surface-2 px-4 py-3 text-heading"
                style={{ top, left }}
            >
                {lemma}
            </div>
        </>
    );
}