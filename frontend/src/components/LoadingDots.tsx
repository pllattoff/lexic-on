// Loading indicator for buttons.
// The dots appear to the right of the text without shifting it.
export default function LoadingDots() {
    return (
        <span
            aria-hidden="true"
            className="absolute bottom-[0.3em] left-full ml-1 inline-flex gap-[2.5px] overflow-hidden animate-dots motion-reduce:animate-none"
        >
            {[0, 1, 2].map((index) => (
                <span key={index} className="size-[3.75px] shrink-0 rounded-full bg-current" />
            ))}
        </span>
    );
}