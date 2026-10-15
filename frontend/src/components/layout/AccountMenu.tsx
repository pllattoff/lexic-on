import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { logout } from "../../api/authApi.ts";
import GithubIcon from "../icons/GithubIcon.tsx";
import LoadingOverlay from "../ui/LoadingOverlay.tsx";
import type { CurrentUser } from "../../types/auth.ts";

type AccountMenuProps = {
    currentUser: CurrentUser | null;
    userLoading: boolean;
};

export default function AccountMenu({ currentUser, userLoading }: Readonly<AccountMenuProps>) {
    const { t } = useTranslation();
    // True from the click on login/logout until the page is left or the request fails
    const [authPending, setAuthPending] = useState(false);

    // When the user returns from GitHub login using the browser's back-forward cache,
    // the page is restored with authPending still true, so reset it on pageshow.
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

    if (userLoading) return overlay;

    if (!currentUser) {
        return (
            <>
                {/* Navigates to Spring Security's OAuth2 endpoint, which redirects to GitHub for authentication
                    and then back to the frontend URL configured in SecurityConfig */}
                <a
                    href="/oauth2/authorization/github"
                    onClick={() => setAuthPending(true)}
                    className="flex items-center gap-2 text-sm"
                >
                    <GithubIcon />
                    <span>{t("account.continueWithGithub")}</span>
                </a>
                {overlay}
            </>
        );
    }

    return (
        <div className="flex items-center gap-4">
            <span className="text-sm">{t("account.greeting", { login: currentUser.login })}</span>
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