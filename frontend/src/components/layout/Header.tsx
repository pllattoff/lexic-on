import { Link } from "react-router";
import MenuIcon from "../icons/MenuIcon.tsx";
import AccountMenu from "./AccountMenu.tsx";
import type { CurrentUser } from "../../types/auth.ts";

type HeaderProps = {
    currentUser: CurrentUser | null;
    userLoading: boolean;
    sidebarOpen: boolean;
    onMenuToggle: () => void;
};

export default function Header({
                                   currentUser,
                                   userLoading,
                                   sidebarOpen,
                                   onMenuToggle,
                               }: Readonly<HeaderProps>) {
    return (
        <header className="sticky top-0 z-10 flex h-header items-center justify-between bg-header px-4 shadow-md shadow-black/40">
            <div className="flex items-center gap-3">
                <button
                    type="button"
                    onClick={onMenuToggle}
                    aria-label="Toggle menu"
                    aria-expanded={sidebarOpen}
                    className="rounded-md p-1 text-body hover:text-heading"
                >
                    <MenuIcon />
                </button>

                <Link to="/" className="text-lg font-semibold text-heading">lexic.on</Link>
            </div>

            <AccountMenu currentUser={currentUser} userLoading={userLoading} />
        </header>
    );
}