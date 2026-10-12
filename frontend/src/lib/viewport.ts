// Uses the same 768px breakpoint as Tailwind's md variant for responsive sidebar layout
const DESKTOP_VIEWPORT_QUERY = "(min-width: 768px)";

export function isDesktopViewport(): boolean {
    return globalThis.matchMedia(DESKTOP_VIEWPORT_QUERY).matches;
}

// Calls the listener when the window crosses the breakpoint; returns a function that unsubscribes
export function onDesktopViewportChange(listener: (isDesktop: boolean) => void): () => void {
    const mediaQuery = globalThis.matchMedia(DESKTOP_VIEWPORT_QUERY);

    function handleChange(event: MediaQueryListEvent) {
        listener(event.matches);
    }

    mediaQuery.addEventListener("change", handleChange);

    return () => mediaQuery.removeEventListener("change", handleChange);
}
