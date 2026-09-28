import { useEffect, useState } from "react";
import { getCurrentUser, logout } from "../api/authApi.ts";
import type { CurrentUser } from "../types/auth.ts";
import GithubIcon from "./icons/GithubIcon.tsx";

export default function AccountMenu() {
    const [me, setMe] = useState<CurrentUser | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        getCurrentUser()
            .then(setMe)
            .finally(() => setLoading(false));
    }, []);
    // [] - runs once after the initial render

    async function handleLogout() {
        const res = await logout();

        if (res.ok) {
            setMe(null);
        } else {
            console.error("Logout failed");
        }
    }

    // The initial rendering of the component returns null
    // The component re-renders after useEffect with [] runs and updates the loading state
    if (loading) return null;

    if (!me) {
        return (
            <a href="/oauth2/authorization/github" className="flex items-center gap-2 text-sm">
                <GithubIcon />
                Continue with GitHub
            </a>
        );
    }

    return (
        <div className="flex items-center gap-4">
            <span className="text-sm">Hello, {me.login}</span>
            <button onClick={handleLogout} title={me.email ?? me.login}>
                {me.avatarUrl && (
                    <img
                        src={me.avatarUrl}
                        alt={me.login}
                        width={32}
                        height={32}
                        className="rounded-full"
                    />
                )}
            </button>
        </div>
    );
}