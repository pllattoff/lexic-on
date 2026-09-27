import AccountMenu from "./AccountMenu.tsx";
import {INPUT_TEXT_STORAGE_KEY} from "../constants/storage.ts";

export default function Header() {
    function handleLogoClick() {
        sessionStorage.removeItem(INPUT_TEXT_STORAGE_KEY);
    }

    return (
        <header className="flex items-center justify-between p-4">
            <h1 className="text-lg font-semibold">
                <a href="/" onClick={handleLogoClick}>lexic.on</a>
            </h1>
            <AccountMenu />
        </header>
    );
}
