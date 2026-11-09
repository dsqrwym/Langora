import {Navigate} from "react-router-dom";
import {getLanguageOrDefault} from "@/i18n/paths.ts";

export const RootRedirect = () => {
    const language = getLanguageOrDefault(location.pathname);
    return <Navigate to={`/${language}`} replace={true}/>;
};