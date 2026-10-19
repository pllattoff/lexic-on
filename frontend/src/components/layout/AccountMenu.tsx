import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { logout } from "../../api/authApi.ts";
import LoginDialog from "./LoginDialog.tsx";
import UserAvatar from "./UserAvatar.tsx";
import ConfirmDialog from "../ui/ConfirmDialog.tsx";
import LoadingOverlay from "../ui/LoadingOverlay.tsx";
import { useAsyncAction } from "../../hooks/useAsyncAction.ts";
import { REQUEST_ERRORS } from "../../lib/requestErrors.ts";
import type { CurrentUser } from "../../types/auth.ts";

type AccountMenuProps = {
    currentUser: CurrentUser | null;
    userLoading: boolean;
};

export default function AccountMenu({ currentUser, userLoading }: Readonly<AccountMenuProps>) {
    const { t } = useTranslation();
    // True from choosing a login provider until the page is left
    const [loginPending, setLoginPending] = useState(false);
    const [loginDialogOpen, setLoginDialogOpen] = useState(false);
    const [logoutDialogOpen, setLogoutDialogOpen] = useState(false);
    const { pending: logoutPending, errorMessage: logoutError, runAction, clearError: clearLogoutError } = useAsyncAction();

    // When the user returns from the provider's login using the browser's back-forward cache,
    // the page is restored with loginPending still true, so reset it on pageshow.
    useEffect(() => {
        function handlePageShow(event: PageTransitionEvent) {
            if (event.persisted) setLoginPending(false);
        }

        globalThis.addEventListener("pageshow", handlePageShow);
        return () => globalThis.removeEventListener("pageshow", handlePageShow);
    }, []);

    function openLogoutDialog() {
        clearLogoutError();
        setLogoutDialogOpen(true);
    }

    async function handleLogoutConfirm() {
        await runAction(async () => {
            const res = await logout();
            if (!res.ok) throw new Error("Logout failed");

            // Reload the page to clear the logged-out user's state
            globalThis.location.reload();
        }, t(REQUEST_ERRORS.logout));
    }

    function handleProviderSelect() {
        // Close the dialog first: an open modal dialog would cover the loading overlay
        setLoginDialogOpen(false);
        setLoginPending(true);
    }

    const overlay = <LoadingOverlay visible={userLoading || loginPending} />;

    if (userLoading) return overlay;

    if (!currentUser) {
        return (
            <>
                <button
                    type="button"
                    onClick={() => setLoginDialogOpen(true)}
                    className="cursor-pointer rounded-md px-2 py-1.5 text-sm text-body transition hover:text-heading focus-visible:outline-2 focus-visible:outline-accent"
                >
                    {t("account.logIn")}
                </button>
                {loginDialogOpen && (
                    <LoginDialog onClose={() => setLoginDialogOpen(false)} onProviderSelect={handleProviderSelect} />
                )}
                {overlay}
            </>
        );
    }

    return (
        <div className="flex items-center gap-4">
            <span className="text-sm">{t("account.greeting", { name: currentUser.name })}</span>
            <button
                type="button"
                onClick={openLogoutDialog}
                aria-label={t("account.logOut")}
                title={currentUser.email ?? currentUser.name}
                className="cursor-pointer rounded-full transition hover:shadow-[0_0_3px_1px] hover:shadow-heading/30 focus-visible:outline-2 focus-visible:outline-accent"
            >
                <UserAvatar avatarUrl={currentUser.avatarUrl} name={currentUser.name} />
            </button>
            {logoutDialogOpen && (
                <ConfirmDialog
                    title={t("account.logOutTitle")}
                    message={t("account.logOutMessage")}
                    confirmLabel={t("account.logOut")}
                    pending={logoutPending}
                    errorMessage={logoutError}
                    onConfirm={() => void handleLogoutConfirm()}
                    onCancel={() => setLogoutDialogOpen(false)}
                />
            )}
            {overlay}
        </div>
    );
}