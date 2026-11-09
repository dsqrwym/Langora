import {useAuth} from "@/lib/auth";
import {Navigate, Outlet, useLocation} from "react-router-dom";
import {FullScreenLoader} from "@/components/ui/full-screen-loader.tsx";
import {buildLocalizedPath, getLanguageOrDefault} from "@/i18n/paths.ts";

/**
 * RequireAuth component is for protecting routes. It will redirect the user to the login page if they are not authenticated.
 */
export const RequireAuth = () => {
    const {status} = useAuth()
    const location = useLocation();
    const lang = getLanguageOrDefault();


    if (status === "checking") {
        return <FullScreenLoader/>;
    }

    if (status === "anonymous") {
        const from = `${location.pathname}${location.search}`;
        return <Navigate
            to={buildLocalizedPath(lang, "/admin/login")}
            replace state={{from}}
        />;
    }

    return <Outlet/>;
}