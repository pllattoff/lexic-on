export default function SpinnerIcon({ size = 18 }: Readonly<{ size?: number }>) {
    return (
        <svg
            viewBox="0 0 24 24"
            width={size}
            height={size}
            fill="none"
            stroke="currentColor"
            strokeWidth={1.5}
            strokeLinecap="round"
            className="animate-spin"
            aria-hidden="true"
        >
            <circle cx="12" cy="12" r="9" className="opacity-25" />
            <path d="M21 12a9 9 0 0 0-9-9" />
        </svg>
    );
}
