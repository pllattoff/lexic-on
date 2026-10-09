import { useEffect, useState } from "react";
import GithubIcon from "./icons/GithubIcon.tsx";
import LoadingOverlay from "./LoadingOverlay.tsx";
import { logout } from "../api/authApi.ts";
import type { CurrentUser } from "../types/auth.ts";

type AccountMenuProps = {
    currentUser: CurrentUser | null;
    userLoading: boolean;
};

export default function AccountMenu({ currentUser, userLoading }: Readonly<AccountMenuProps>) {
    // True from the click on login/logout until the page is left or the request fails
    const [authPending, setAuthPending] = useState(false);

    // Going back from the GitHub login page restores this page from the browser cache
    // with authPending still true, so it has to be reset
    useEffect(() => {
        function handlePageShow(event: PageTransitionEvent) {
            if (event.persisted) setAuthPending(false);
        }

        globalThis.addEventListener("pageshow", handlePageShow);
        return () => globalThis.removeEventListener("pageshow", handlePageShow);
    }, []);

    async function handleLogout() {
        setAuthPending(true);
        const res = await logout();

        if (res.ok) {
            // Reload the page to clear the logged-out user's state
            globalThis.location.reload();
        } else {
            console.error("Logout failed");
            setAuthPending(false);
        }
    }

    const overlay = <LoadingOverlay visible={userLoading || authPending} />;

    // getCurrentUser() runs after the initial render.
    // The component re-renders when userLoading changes.
    if (userLoading) return overlay;

    if (!currentUser) {
        return (
            <>
                <a
                    href="/oauth2/authorization/github"
                    onClick={() => setAuthPending(true)}
                    className="flex items-center gap-2 text-sm"
                >
                    <GithubIcon />
                    Continue with GitHub
                </a>
                {overlay}
            </>
        );
    }

    return (
        <div className="flex items-center gap-4">
            <span className="text-sm">Hello, {currentUser.login}</span>
            <button onClick={handleLogout} title={currentUser.email ?? currentUser.login}>
                {currentUser.avatarUrl && (
                    <img
                        src={currentUser.avatarUrl}
                        alt={currentUser.login}
                        width={32}
                        height={32}
                        className="rounded-full"
                    />
                )}
            </button>
            {overlay}
        </div>
    );
}