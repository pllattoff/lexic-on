import { useTranslation } from "react-i18next";
import { Link } from "react-router";
import { PAGE_CONTAINER_CLASSNAME, PAGE_TITLE_CLASSNAME } from "../lib/pageStyles.ts";

export default function NotFoundPage() {
    const { t } = useTranslation();

    return (
        <div className={PAGE_CONTAINER_CLASSNAME}>
            <h1 className={PAGE_TITLE_CLASSNAME}>{t("notFound.title")}</h1>
            <Link to="/" className="underline">{t("notFound.backToText")}</Link>
        </div>
    );
}