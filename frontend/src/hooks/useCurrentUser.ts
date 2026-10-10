import { useEffect, useState } from "react";
import { getCurrentUser } from "../api/authApi.ts";
import type { CurrentUser } from "../types/auth.ts";

export function useCurrentUser() {
    const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
    const [userLoading, setUserLoading] = useState(true);

    useEffect(() => {
        async function loadCurrentUser() {
            try {
                // wait for the request to complete and get the user instead of a Promise
                const user = await getCurrentUser();
                setCurrentUser(user);
            } catch (error) {
                console.error("Failed to load current user:", error);
            } finally {
                setUserLoading(false);
            }
        }

        // async functions return a Promise, but useEffect must return only a cleanup function or nothing,
        // so we explicitly discard the returned Promise with void
        void loadCurrentUser();
    }, []);
    // [] - runs once after the initial render

    return { currentUser, userLoading };
}