import { useTranslation } from "react-i18next";
import GithubIcon from "../icons/GithubIcon.tsx";
import GoogleIcon from "../icons/GoogleIcon.tsx";
import ModalDialog from "../ui/ModalDialog.tsx";

type LoginDialogProps = {
    onClose: () => void;
    // Called when a provider is chosen, right before the browser goes to that provider's login
    onProviderSelect: () => void;
};

const PROVIDER_LINK_CLASSNAME =
    "flex items-center justify-center gap-3 rounded-md border border-outline px-4 py-3 text-heading transition hover:border-outline-strong hover:bg-outline/30";

// A modal window with the sign-in options. Each option navigates to Spring Security's OAuth2 endpoint,
// which redirects to the provider for authentication and then back to the frontend URL configured in SecurityConfig.
export default function LoginDialog({ onClose, onProviderSelect }: Readonly<LoginDialogProps>) {
    const { t } = useTranslation();

    return (
        <ModalDialog title={t("account.logIn")} onClose={onClose}>
            <div className="mt-6 flex flex-col gap-3">
                <a href="/oauth2/authorization/google" onClick={onProviderSelect} className={PROVIDER_LINK_CLASSNAME}>
                    <GoogleIcon />
                    <span>{t("account.continueWithGoogle")}</span>
                </a>
                <a href="/oauth2/authorization/github" onClick={onProviderSelect} className={PROVIDER_LINK_CLASSNAME}>
                    <GithubIcon />
                    <span>{t("account.continueWithGithub")}</span>
                </a>
            </div>

            <button
                type="button"
                onClick={onClose}
                className="mt-3 w-full cursor-pointer rounded-md px-4 py-2 text-sm text-body transition hover:text-heading focus-visible:outline-2 focus-visible:outline-accent"
            >
                {t("common.cancel")}
            </button>
        </ModalDialog>
    );
}
