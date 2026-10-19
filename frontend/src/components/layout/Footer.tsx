import { useTranslation } from "react-i18next";

export default function Footer() {
    const { t } = useTranslation();

    return (
        <footer className="flex flex-wrap items-center justify-center gap-x-2 gap-y-1 px-4 pb-4 pt-6 text-sm text-body/60">
            <span>© 2026 lexic.on</span>
            <span aria-hidden="true" className="text-[10px]">•</span>
            {/* A plain link: privacy.html is a static file, not a route of the SPA */}
            <a href="/privacy.html" target="_blank" rel="noopener noreferrer" className="hover:underline">
                {t("common.privacyPolicy")}
            </a>
        </footer>
    );
}
