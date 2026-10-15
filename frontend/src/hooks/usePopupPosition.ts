import { useLayoutEffect, type RefObject } from "react";

// Minimal distance between the popup and the edges of the screen
const SCREEN_MARGIN_PX = 8;

// Distance between the popup and the element it belongs to
const ANCHOR_GAP_PX = 6;

// Position of the element the popup belongs to, in screen coordinates
type PopupAnchor = {
    top: number;
    bottom: number;
    left: number;
};

// Places a fixed popup under its anchor. When it would stick out of the screen, shifts it to the left,
// and puts it above the anchor if there is no room below. If it fits neither above nor below,
// it takes the larger side and scrolls inside.
// Runs after every render, because the size changes with the content (e.g. an error message).
export function usePopupPosition(ref: RefObject<HTMLElement | null>, anchor: PopupAnchor) {
    useLayoutEffect(() => {
        const popup = ref.current;
        if (!popup) return;

        const maxLeft = document.documentElement.clientWidth - popup.offsetWidth - SCREEN_MARGIN_PX;
        popup.style.left = `${Math.max(SCREEN_MARGIN_PX, Math.min(anchor.left, maxLeft))}px`;

        // Measure the natural height, without the limit set by the previous render
        popup.style.maxHeight = "";
        const popupHeight = popup.offsetHeight;
        const spaceBelow = document.documentElement.clientHeight - anchor.bottom - ANCHOR_GAP_PX - SCREEN_MARGIN_PX;
        const spaceAbove = anchor.top - ANCHOR_GAP_PX - SCREEN_MARGIN_PX;

        if (popupHeight <= spaceBelow) {
            popup.style.top = `${anchor.bottom + ANCHOR_GAP_PX}px`;
        } else if (popupHeight <= spaceAbove) {
            popup.style.top = `${anchor.top - ANCHOR_GAP_PX - popupHeight}px`;
        } else if (spaceBelow >= spaceAbove) {
            popup.style.top = `${anchor.bottom + ANCHOR_GAP_PX}px`;
            popup.style.maxHeight = `${spaceBelow}px`;
        } else {
            popup.style.top = `${SCREEN_MARGIN_PX}px`;
            popup.style.maxHeight = `${spaceAbove}px`;
        }
    });
}
