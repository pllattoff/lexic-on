import Footer from "./components/Footer.tsx";
import Header from "./components/Header.tsx";
import TextPage from "./components/TextPage.tsx";

function App() {
    return (
        <div className="flex min-h-screen flex-col">
            <Header />
            <main className="flex-1">   {/* Fill the remaining vertical space between header and footer */}
                <TextPage />
            </main>
            <Footer />
        </div>
    );
}

export default App;