import { Outlet } from "react-router";

type DictionariesGateProps = {
    isAuthenticated: boolean;
    userLoading: boolean;
};

// Only authenticated users can access dictionary pages; guests see a login invitation
export default function DictionariesGate({ isAuthenticated, userLoading }: Readonly<DictionariesGateProps>) {
    // Nothing to show until it is known whether the user is logged in
    if (userLoading) return null;

    if (!isAuthenticated) {
        return (
            <div className="mx-auto max-w-3xl p-4">
                <h1 className="mb-2 text-lg font-medium text-heading">Dictionaries</h1>
                <p>Log in to access your personal dictionaries.</p>
            </div>
        );
    }

    return <Outlet />;
}