import AccountMenu from "./AccountMenu.tsx";
import {INPUT_TEXT_STORAGE_KEY} from "../constants/storage.ts";
import type { CurrentUser } from "../types/auth.ts";

type HeaderProps = {
    currentUser: CurrentUser | null;
    userLoading: boolean;
};

function handleLogoClick() {
    sessionStorage.removeItem(INPUT_TEXT_STORAGE_KEY);
}

export default function Header({ currentUser, userLoading }: Readonly<HeaderProps>) {
    return (
        <header className="flex items-center justify-between p-4">
            <h1 className="text-lg font-semibold">
                <a href="/" onClick={handleLogoClick}>lexic.on</a>
            </h1>
            <AccountMenu currentUser={currentUser} userLoading={userLoading} />
        </header>
    );
}