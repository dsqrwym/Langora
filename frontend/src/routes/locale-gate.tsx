import {Navigate, Outlet, useLocation, useParams} from "react-router-dom";
import {useTranslation} from "react-i18next";
import {isSupportedLanguage} from "@/i18n/languages.ts";
import {useEffect} from "react";
import {buildLocalizedPath, getLanguageOrDefault} from "@/i18n/paths.ts";

/**
 * LocaleGate
 * @constructor
 */
export const LocaleGate = () => {
    const {lang} = useParams();
    const location = useLocation();
    const {i18n} = useTranslation();

    const valid = isSupportedLanguage(lang);

    // 类似 LaunchEffect
    useEffect(() => {
        if (!valid) return;
        if (i18n.resolvedLanguage === lang) return;
        void i18n.changeLanguage(lang);
    }, [i18n, lang, valid]);

    if (!valid) {
        const rest = location.pathname.split("/").slice(2).join("/");
        const target = buildLocalizedPath(
            getLanguageOrDefault(location.pathname),
            rest ? `/${rest}` : "/",
        );
        return <Navigate to={target} replace={true}/>;
    }

    return <Outlet/>;
}