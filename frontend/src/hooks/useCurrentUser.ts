import { useEffect, useState } from "react";
import { getCurrentUser } from "../api/authApi.ts";
import type { CurrentUser } from "../types/auth.ts";

export function useCurrentUser() {
    const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
    const [userLoading, setUserLoading] = useState(true);

    useEffect(() => {
        getCurrentUser()
            .then(setCurrentUser)
            .finally(() => setUserLoading(false));
    }, []);
    // [] - runs once after the initial render

    return { currentUser, userLoading };
}