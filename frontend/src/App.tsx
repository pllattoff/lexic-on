import Footer from "./components/Footer.tsx";
import Header from "./components/Header.tsx";
import TextPage from "./components/TextPage.tsx";
import { useCurrentUser } from "./hooks/useCurrentUser.ts";

function App() {
    const { currentUser, userLoading } = useCurrentUser();

    return (
        <div className="flex min-h-screen flex-col">
            <Header currentUser={currentUser} userLoading={userLoading} />
            <main className="flex-1">   {/* Fill the remaining vertical space between header and footer */}
                <TextPage isAuthenticated={currentUser !== null} />
            </main>
            <Footer />
        </div>
    );
}

export default App;