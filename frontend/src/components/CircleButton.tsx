import type { ReactNode } from "react";

// Colors of the circle at rest and on hover; Tailwind finds classes only in complete strings
const VARIANT_CLASSNAMES = {
    // On the dark page and its panels
    default: "bg-surface-2 hover:bg-outline/55",
    // On the lighter popups, where the default circle would blend into the background
    inPopup: "bg-outline/40 hover:bg-outline/60",
};

type CircleButtonProps = {
    // Used as the accessible name and as the tooltip
    label: string;
    variant?: keyof typeof VARIANT_CLASSNAMES;
    disabled?: boolean;
    // Positioning classes of the place where the button is used
    className?: string;
    onClick: () => void;
    children: ReactNode;
};

// A round icon button
export default function CircleButton({
                                         label,
                                         variant = "default",
                                         disabled = false,
                                         className = "",
                                         onClick,
                                         children,
                                     }: Readonly<CircleButtonProps>) {
    return (
        <button
            type="button"
            disabled={disabled}
            onClick={onClick}
            aria-label={label}
            title={label}
            className={`flex size-7 items-center justify-center rounded-full border border-transparent text-body/65 transition hover:border-outline-strong/40 hover:text-body focus-visible:text-body disabled:cursor-not-allowed ${VARIANT_CLASSNAMES[variant]} ${className}`}
        >
            {children}
        </button>
    );
}
