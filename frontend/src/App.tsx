import AccountMenu from "./components/AccountMenu.tsx";
import TextPage from "./components/TextPage.tsx";

function App() {
    return (
        <div>
            <header className="flex items-center justify-between p-4">
                <h1 className="text-lg font-semibold">lexic.on</h1>
                <AccountMenu />
            </header>
            <main className="p-4">
                <TextPage />
            </main>
        </div>
    );
}

export default App;