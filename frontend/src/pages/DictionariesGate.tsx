import { useTranslation } from "react-i18next";
import { Outlet } from "react-router";
import { PAGE_CONTAINER_CLASSNAME, PAGE_TITLE_CLASSNAME } from "../lib/pageStyles.ts";

type DictionariesGateProps = {
    isAuthenticated: boolean;
    userLoading: boolean;
};

// Only authenticated users can access dictionary pages; guests see a login invitation
export default function DictionariesGate({ isAuthenticated, userLoading }: Readonly<DictionariesGateProps>) {
    const { t } = useTranslation();

    // Nothing to show until it is known whether the user is logged in
    if (userLoading) return null;

    if (!isAuthenticated) {
        return (
            <div className={PAGE_CONTAINER_CLASSNAME}>
                <h1 className={PAGE_TITLE_CLASSNAME}>{t("common.dictionaries")}</h1>
                <p>{t("dictionariesGate.loginPrompt")}</p>
            </div>
        );
    }

    return <Outlet />;
}