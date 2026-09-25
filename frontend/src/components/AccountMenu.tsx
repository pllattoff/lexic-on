import { useEffect, useState } from "react";
import { fetchWithCsrf } from "../lib/csrf.ts";

type Me = {
    login: string;
    email: string | null;
    avatarUrl: string | null;
};

export default function AccountMenu() {
    const [me, setMe] = useState<Me | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetch("/api/auth/csrf")
            .then(() => fetch("/api/auth/me"))
            .then((res) => (res.ok ? res.json() : null))
            .then(setMe)
            .finally(() => setLoading(false));
    }, []);

    async function postLogout() {
        return fetchWithCsrf("/logout", {
            method: "POST",
            credentials: "include",
        });
    }

    async function handleLogout() {
        const res = await postLogout();

        if (res.ok) {
            setMe(null);
        } else {
            console.error("Logout failed");
        }
    }

    if (loading) return null;

    if (!me) {
        return <a href="/oauth2/authorization/github">Continue with GitHub</a>;
    }

    return (
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
    );
}