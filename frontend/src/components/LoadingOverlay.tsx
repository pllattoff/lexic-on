import SpinnerIcon from "./icons/SpinnerIcon.tsx";

// Full-screen loading indicator displayed over the app content
export default function LoadingOverlay({ visible }: Readonly<{ visible: boolean }>) {
    if (!visible) return null;

    return (
        <div
            role="status"
            aria-label="Loading"
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 text-accent"
        >
            <SpinnerIcon size={48} />
        </div>
    );
}
