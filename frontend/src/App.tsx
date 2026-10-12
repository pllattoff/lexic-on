import { useEffect, useState } from "react";
import { Navigate, Route, Routes } from "react-router";
import Footer from "./components/layout/Footer.tsx";
import Header from "./components/layout/Header.tsx";
import Sidebar from "./components/layout/Sidebar.tsx";
import { useCurrentUser } from "./hooks/useCurrentUser.ts";
import { useDictionaries } from "./hooks/useDictionaries.ts";
import { useTextSession } from "./hooks/useTextSession.ts";
import type { SourceLanguage, TargetLanguage } from "./lib/languages.ts";
import { isDesktopViewport, onDesktopViewportChange } from "./lib/viewport.ts";
import DictionariesGate from "./pages/DictionariesGate.tsx";
import DictionariesPage from "./pages/DictionariesPage.tsx";
import DictionaryPage from "./pages/DictionaryPage.tsx";
import NotFoundPage from "./pages/NotFoundPage.tsx";
import TextPage from "./pages/TextPage.tsx";
import type { WordsStatusChangeHandler } from "./types/dictionary.ts";

function App() {
    const { currentUser, userLoading } = useCurrentUser();
    const isAuthenticated = currentUser !== null;
    const textSession = useTextSession();
    const { dictionaries, handleVocabularyChange } = useDictionaries(isAuthenticated);
    // Open on wide screens, closed on small ones, where it covers the content
    const [sidebarOpen, setSidebarOpen] = useState(isDesktopViewport);

    // Follow the window size: the sidebar collapses when the window gets narrow and opens again when it gets wide
    useEffect(() => onDesktopViewportChange(setSidebarOpen), []);

    // After words got a new status (or were removed from the vocabulary): refresh the dictionaries
    // and show the new status in an open text. Used by every page that changes the statuses.
    const handleWordsStatusChange: WordsStatusChangeHandler = (sourceLanguage, targetLanguage, lemmas, status) => {
        handleVocabularyChange(sourceLanguage, targetLanguage, status === null ? "deleted" : "saved");
        textSession.updateLemmasStatusOfPair(sourceLanguage, targetLanguage, lemmas, status);
    };

    // After a dictionary is deleted: refresh the dictionaries and reset the word statuses of an open text of that pair
    function handleDictionaryDeleted(sourceLanguage: SourceLanguage, targetLanguage: TargetLanguage) {
        handleVocabularyChange(sourceLanguage, targetLanguage, "deleted");
        textSession.resetStatuses(sourceLanguage, targetLanguage);
    }

    // On small screens the panel closes after a link is chosen
    function handleSidebarNavigate() {
        if (!isDesktopViewport()) setSidebarOpen(false);
    }

    return (
        <div className="flex min-h-screen flex-col">
            <Header
                currentUser={currentUser}
                userLoading={userLoading}
                sidebarOpen={sidebarOpen}
                onMenuToggle={() => setSidebarOpen((open) => !open)}
            />
            <div className="flex flex-1">
                {sidebarOpen && (
                    <Sidebar
                        isAuthenticated={isAuthenticated}
                        dictionaries={dictionaries}
                        onNavigate={handleSidebarNavigate}
                        onClose={() => setSidebarOpen(false)}
                    />
                )}
                {/* The footer is inside the right column, so the sticky sidebar can reach the very bottom of the page */}
                <div className="flex min-w-0 flex-1 flex-col">
                    <main className="flex-1">   {/* Fill the remaining space above the footer */}
                        <Routes>
                            <Route
                                path="/"
                                element={
                                    <TextPage
                                        isAuthenticated={isAuthenticated}
                                        textSession={textSession}
                                        onWordsStatusChange={handleWordsStatusChange}
                                    />
                                }
                            />
                            <Route
                                path="/dictionaries"
                                element={<DictionariesGate isAuthenticated={isAuthenticated} userLoading={userLoading} />}
                            >
                                <Route index element={<DictionariesPage dictionaries={dictionaries} onDictionaryDeleted={handleDictionaryDeleted} />} />
                                {/* A language pair without a status opens its first tab */}
                                <Route path=":pair" element={<Navigate to="unknown" replace />} />
                                <Route
                                    path=":pair/:status"
                                    element={<DictionaryPage onWordsStatusChange={handleWordsStatusChange} />}
                                />
                            </Route>
                            <Route path="*" element={<NotFoundPage />} />
                        </Routes>
                    </main>
                    <Footer />
                </div>
            </div>
        </div>
    );
}

export default App;