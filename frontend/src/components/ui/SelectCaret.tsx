import ChevronDownIcon from "../icons/ChevronDownIcon.tsx";

type SelectCaretProps = {
    size?: number;
    // Classes of the place where the caret is used, e.g. a nudge to line it up with the text
    className?: string;
};

// The caret of a select whose native element is transparent and drawn over this control.
// It turns over while the list is open; the control that holds the select must have the "group" class.
// Browsers without the :open pseudo-class keep the caret as is.
export default function SelectCaret({ size, className = "" }: Readonly<SelectCaretProps>) {
    return (
        <span className={`transition-transform group-has-[select:open]:rotate-180 ${className}`}>
            <ChevronDownIcon size={size} />
        </span>
    );
}
