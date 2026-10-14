import { useEffect, useState } from "react";
import { isDesktopViewport, subscribeToDesktopViewport } from "../lib/viewport.ts";

// Manages whether the sidebar is open, following the window size
export function useSidebar() {
    const [sidebarOpen, setSidebarOpen] = useState(isDesktopViewport);

    // Subscribes to viewport breakpoint changes and updates sidebarOpen when the breakpoint is crossed
    useEffect(() => subscribeToDesktopViewport(setSidebarOpen), []);

    function toggleSidebar() {
        setSidebarOpen((open) => !open);
    }

    function closeSidebar() {
        setSidebarOpen(false);
    }

    // On small screens the panel closes after a link is chosen
    function closeSidebarOnMobile() {
        if (!isDesktopViewport()) closeSidebar();
    }

    return { sidebarOpen, toggleSidebar, closeSidebar, closeSidebarOnMobile };
}
