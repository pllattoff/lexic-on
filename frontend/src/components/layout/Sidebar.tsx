import { NavLink } from "react-router";
import DictionariesIcon from "../icons/DictionariesIcon.tsx";
import TextIcon from "../icons/TextIcon.tsx";
import { dictionaryPath, dictionaryStatusPath } from "../../lib/dictionaryRoutes.ts";
import { LANGUAGE_LABELS } from "../../lib/languages.ts";
import { STATUS_LABELS, VOCABULARY_STATUSES } from "../../lib/vocabulary.ts";
import { STATUS_STYLES } from "../../lib/vocabularyStyles.ts";
import type { DictionarySummary } from "../../types/dictionary.ts";

type SidebarProps = {
    isAuthenticated: boolean;
    dictionaries: DictionarySummary[] | null;
    onNavigate: () => void;
    onClose: () => void;
};

// The left border is transparent by default, so the active marker appears without shifting the text
const LINK_CLASSNAME = "flex items-center gap-2 rounded-r-md border-l-2 px-3 py-1.5 transition-colors";

// isActive is provided by React Router to indicate whether the NavLink matches the current URL
function mainLinkClassName({ isActive }: { isActive: boolean }) {
    const activeClassName = isActive
        ? "border-accent bg-surface-2 text-heading"
        : "border-transparent hover:text-heading";

    return `${LINK_CLASSNAME} ${activeClassName}`;
}

export default function Sidebar({ isAuthenticated, dictionaries, onNavigate, onClose }: Readonly<SidebarProps>) {
    return (
        <>
            {/* Behind the panel on small screens only: a click on the dimmed area closes the panel */}
            <div className="fixed inset-0 z-20 bg-black/50 md:hidden" onClick={onClose} aria-hidden="true" />

            {/* A panel over the content on small screens, a column next to the content from md up */}
            <aside className="fixed inset-y-0 left-0 z-30 w-56 shrink-0 overflow-y-auto border-r border-outline bg-surface p-4 md:sticky md:top-header md:bottom-auto md:z-auto md:h-[calc(100vh-var(--spacing-header))] md:self-start md:bg-transparent">
                <nav className="flex flex-col gap-1 text-body">
                    <NavLink to="/" end onClick={onNavigate} className={mainLinkClassName}>
                        <span className="text-sky-400"><TextIcon /></span>
                        <span>Text</span>
                    </NavLink>

                    <NavLink to="/dictionaries" end onClick={onNavigate} className={mainLinkClassName}>
                        <span className="text-violet-400"><DictionariesIcon /></span>
                        <span>Dictionaries</span>
                    </NavLink>

                    {isAuthenticated && dictionaries && dictionaries.length > 0 && (
                        <div className="mt-4 flex flex-col gap-3 border-t border-outline pt-4">
                            <h2 className="px-3 text-xs font-medium uppercase tracking-wider text-body/60">
                                Your dictionaries
                            </h2>

                            {dictionaries.map((dictionary) => (
                                <DictionaryLinks key={dictionaryPath(dictionary.sourceLanguage, dictionary.targetLanguage)}
                                                 dictionary={dictionary}
                                                 onNavigate={onNavigate} />
                            ))}
                        </div>
                    )}
                </nav>
            </aside>
        </>
    );
}

function DictionaryLinks({ dictionary, onNavigate }: Readonly<{ dictionary: DictionarySummary; onNavigate: () => void }>) {
    const { sourceLanguage, targetLanguage } = dictionary;

    return (
        <div className="flex flex-col">
            {/* Without React Router's "end" prop, this link also stays active on the pair's status pages */}
            <NavLink to={dictionaryPath(sourceLanguage, targetLanguage)} onClick={onNavigate} className={mainLinkClassName}>
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

                            return `${LINK_CLASSNAME} py-1 text-sm ${statusStyle}`;
                        }}
                    >
                        {STATUS_LABELS[status]}
                    </NavLink>
                ))}
            </div>
        </div>
    );
}