import { useTranslation } from "react-i18next";
import { NavLink } from "react-router";
import DictionariesIcon from "../icons/DictionariesIcon.tsx";
import TextIcon from "../icons/TextIcon.tsx";
import { dictionaryPath, dictionaryStatusPath } from "../../lib/dictionaryRoutes.ts";
import { LANGUAGE_LABELS } from "../../lib/languages.ts";
import { STATUS_LABEL_KEYS, VOCABULARY_STATUSES } from "../../lib/vocabulary.ts";
import { STATUS_STYLES } from "../../lib/vocabularyStyles.ts";
import type { DictionarySummary } from "../../types/dictionary.ts";
import UiLanguageSelect from "./UiLanguageSelect.tsx";

type SidebarProps = {
    isAuthenticated: boolean;
    dictionaries: DictionarySummary[] | null;
    onNavigate: () => void;
    onClose: () => void;
};

// Overlays the content on mobile
const SIDEBAR_BASE_CLASSNAME = "fixed inset-y-0 left-0 z-30 flex w-56 flex-col shrink-0 border-r border-outline bg-surface px-4 pt-4 pb-2";

// Stays sticky beside the content from md up
const SIDEBAR_DESKTOP_CLASSNAME = "md:sticky md:top-header md:bottom-auto md:z-auto md:h-[calc(100vh-var(--spacing-header))] md:self-start md:bg-transparent";

// The left border is transparent by default, so the active marker appears without shifting the text
const NAV_LINK_CLASSNAME = "flex items-center gap-2 rounded-r-md border-l-2 px-3 py-1.5 transition-colors";

// isActive is provided by React Router to indicate whether the NavLink matches the current URL
function mainNavLinkClassName({ isActive }: { isActive: boolean }) {
    const activeClassName = isActive
        ? "border-accent bg-surface-2 text-heading"
        : "border-transparent hover:text-heading";

    return `${NAV_LINK_CLASSNAME} ${activeClassName}`;
}

export default function Sidebar({ isAuthenticated, dictionaries, onNavigate, onClose }: Readonly<SidebarProps>) {
    const { t } = useTranslation();

    return (
        <>
            {/* Dimmed backdrop behind the sidebar on mobile; clicking it closes the sidebar */}
            <div className="fixed inset-0 z-20 bg-black/50 md:hidden" onClick={onClose} aria-hidden="true" />

            <aside className={`${SIDEBAR_BASE_CLASSNAME} ${SIDEBAR_DESKTOP_CLASSNAME}`}>
                {/* Only the navigation scrolls, so the language selector below stays visible */}
                <nav className="flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto text-body">
                    <NavLink to="/" end onClick={onNavigate} className={mainNavLinkClassName}>
                        <span className="text-sky-400"><TextIcon /></span>
                        <span>{t("sidebar.text")}</span>
                    </NavLink>

                    <NavLink to="/dictionaries" end onClick={onNavigate} className={mainNavLinkClassName}>
                        <span className="text-violet-400"><DictionariesIcon /></span>
                        <span>{t("common.dictionaries")}</span>
                    </NavLink>

                    {isAuthenticated && dictionaries && dictionaries.length > 0 && (
                        <div className="mt-4 flex flex-col gap-3 border-t border-outline pt-4">
                            <h2 className="px-3 text-xs font-medium uppercase tracking-wider text-body/60">
                                {t("sidebar.yourDictionaries")}
                            </h2>

                            {dictionaries.map((dictionary) => (
                                <DictionaryLinks key={dictionaryPath(dictionary.sourceLanguage, dictionary.targetLanguage)}
                                                 dictionary={dictionary}
                                                 onNavigate={onNavigate} />
                            ))}
                        </div>
                    )}
                </nav>

                <div className="mt-2 shrink-0 border-t border-outline pt-2">
                    <UiLanguageSelect />
                </div>
            </aside>
        </>
    );
}

function DictionaryLinks({ dictionary, onNavigate }: Readonly<{ dictionary: DictionarySummary; onNavigate: () => void }>) {
    const { t } = useTranslation();
    const { sourceLanguage, targetLanguage } = dictionary;

    return (
        <div className="flex flex-col">
            {/* Without React Router's "end" prop, this link also stays active on the pair's status pages */}
            <NavLink to={dictionaryPath(sourceLanguage, targetLanguage)} onClick={onNavigate} className={mainNavLinkClassName}>
                {LANGUAGE_LABELS[sourceLanguage]} → {LANGUAGE_LABELS[targetLanguage]}
            </NavLink>

            {/* Each status has a stripe of its color on the left; the active one is brighter and has a tinted background */}
            <div className="ml-4 flex flex-col gap-1 pt-1">
                {/* The "end" prop matches only the current route as active, not its subroutes */}
                {VOCABULARY_STATUSES.map((status) => (
                    <NavLink
                        key={status}
                        to={dictionaryStatusPath(sourceLanguage, targetLanguage, status)}
                        end
                        onClick={onNavigate}
                        className={({ isActive }) => {
                            const statusStyle = isActive
                                ? STATUS_STYLES[status].navSelected
                                : STATUS_STYLES[status].nav;

                            return `${NAV_LINK_CLASSNAME} py-1 text-sm ${statusStyle}`;
                        }}
                    >
                        {t(STATUS_LABEL_KEYS[status])}
                    </NavLink>
                ))}
            </div>
        </div>
    );
}